#!/data/data/com.termux/files/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
SDK_DIR="${ANDROID_HOME:-$HOME/android-sdk}"
if [ ! -d "$SDK_DIR" ] && [ -d "$HOME/Android/Sdk" ]; then
    SDK_DIR="$HOME/Android/Sdk"
fi

# Locate tools
AAPT2="$(command -v aapt2 2>/dev/null || true)"
ZIPALIGN="$(command -v zipalign 2>/dev/null || true)"
APKSIGNER="$(command -v apksigner 2>/dev/null || true)"

# Auto-detect latest build-tools and platform if SDK available
BUILD_TOOLS_DIR=$(ls -d "$SDK_DIR/build-tools/"* 2>/dev/null | sort -V | tail -n 1 || true)
BUILD_TOOLS="${BUILD_TOOLS_DIR:-$SDK_DIR/build-tools/34.0.0}"
PLATFORM_DIR=$(ls -d "$SDK_DIR/platforms/android-"* 2>/dev/null | sort -V | tail -n 1 || true)
PLATFORM_JAR="${PLATFORM_DIR:-$SDK_DIR/platforms/android-34}/android.jar"

[ -z "$AAPT2" ] && AAPT2="$BUILD_TOOLS/aapt2"
[ -z "$ZIPALIGN" ] && ZIPALIGN="$BUILD_TOOLS/zipalign"
[ -z "$APKSIGNER" ] && APKSIGNER="$BUILD_TOOLS/apksigner"

if command -v d8 >/dev/null 2>&1; then
    D8_CMD="d8"
elif [ -f "$BUILD_TOOLS/lib/d8.jar" ]; then
    D8_CMD="java -cp $BUILD_TOOLS/lib/d8.jar com.android.tools.r8.D8"
else
    D8_CMD="$BUILD_TOOLS/d8"
fi

KEYSTORE="$PROJECT_DIR/keystore/dodopress-release.keystore"
KEYSTORE_PASS="dodopress123"
KEYSTORE_ALIAS="dodopress"

# Generate permanent keystore if missing
if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkey -v -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -alias "$KEYSTORE_ALIAS" -keypass "$KEYSTORE_PASS" -keyalg RSA -keysize 2048 -validity 25000 -dname "CN=DodoPress Release,O=DodoPress,C=US"
fi

echo "=== 1. Cleaning build directory ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/obj/res"
mkdir -p "$BUILD_DIR/obj/gen"
mkdir -p "$BUILD_DIR/obj/classes"
mkdir -p "$BUILD_DIR/apk"

echo "=== 2. Compiling resources with aapt2 ==="
"$AAPT2" compile --dir "$PROJECT_DIR/app/src/main/res" -o "$BUILD_DIR/obj/resources.zip"

echo "=== 3. Linking resources and generating R.java ==="
"$AAPT2" link \
    -I "$PLATFORM_JAR" \
    --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
    --java "$BUILD_DIR/obj/gen" \
    -o "$BUILD_DIR/apk/res.apk" \
    "$BUILD_DIR/obj/resources.zip"

echo "=== 4. Compiling Java sources ==="
find "$PROJECT_DIR/app/src/main/java" "$BUILD_DIR/obj/gen" -name "*.java" > "$BUILD_DIR/sources.txt"
javac --release 17 -cp "$PLATFORM_JAR" -d "$BUILD_DIR/obj/classes" @"$BUILD_DIR/sources.txt"

echo "=== 5. Dexing bytecode with d8 ==="
find "$BUILD_DIR/obj/classes" -name "*.class" > "$BUILD_DIR/classes.txt"
$D8_CMD --release --min-api 24 --lib "$PLATFORM_JAR" --output "$BUILD_DIR/apk/" @"$BUILD_DIR/classes.txt"

echo "=== 6. Packaging APK ==="
cp "$BUILD_DIR/apk/res.apk" "$BUILD_DIR/apk/unaligned.apk"
cd "$BUILD_DIR/apk"
zip -u -j unaligned.apk classes.dex

# Add assets
mkdir -p "$BUILD_DIR/apk_temp/assets"
cp -r "$PROJECT_DIR/app/src/main/assets/"* "$BUILD_DIR/apk_temp/assets/"
cd "$BUILD_DIR/apk_temp"
zip -u -r "$BUILD_DIR/apk/unaligned.apk" assets/

# Add native libraries (lib/arm64-v8a/*.so)
mkdir -p "$BUILD_DIR/apk_temp/lib/arm64-v8a"
cp "$PROJECT_DIR/app/src/main/lib/arm64-v8a/"*.so "$BUILD_DIR/apk_temp/lib/arm64-v8a/"
zip -u -r "$BUILD_DIR/apk/unaligned.apk" lib/

echo "=== 7. Zipalign ==="
"$ZIPALIGN" -f -p 4 "$BUILD_DIR/apk/unaligned.apk" "$BUILD_DIR/apk/aligned.apk"

echo "=== 8. Signing APK ==="
"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias "$KEYSTORE_ALIAS" \
    --ks-pass "pass:$KEYSTORE_PASS" \
    --key-pass "pass:$KEYSTORE_PASS" \
    --out "$PROJECT_DIR/DodoPress.apk" \
    "$BUILD_DIR/apk/aligned.apk"

echo "=== Build Complete! Created $PROJECT_DIR/DodoPress.apk ==="
ls -lh "$PROJECT_DIR/DodoPress.apk"
