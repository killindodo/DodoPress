#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
SDK_DIR="${ANDROID_HOME:-$HOME/Android/Sdk}"

# Auto-detect latest build-tools and platform
BUILD_TOOLS_DIR=$(ls -d "$SDK_DIR/build-tools/"* 2>/dev/null | sort -V | tail -n 1)
BUILD_TOOLS="${BUILD_TOOLS_DIR:-$SDK_DIR/build-tools/36.0.0}"
PLATFORM_DIR=$(ls -d "$SDK_DIR/platforms/android-"* 2>/dev/null | sort -V | tail -n 1)
PLATFORM_JAR="${PLATFORM_DIR:-$SDK_DIR/platforms/android-34}/android.jar"

KEYSTORE="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"

# Generate debug keystore if missing
if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkey -v -keystore "$KEYSTORE" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

echo "=== 1. Cleaning build directory ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/obj/res"
mkdir -p "$BUILD_DIR/obj/gen"
mkdir -p "$BUILD_DIR/obj/classes"
mkdir -p "$BUILD_DIR/apk"

echo "=== 2. Compiling resources with aapt2 ==="
"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/app/src/main/res" -o "$BUILD_DIR/obj/resources.zip"

echo "=== 3. Linking resources and generating R.java ==="
"$BUILD_TOOLS/aapt2" link \
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
"$BUILD_TOOLS/d8" --release --min-api 24 --lib "$PLATFORM_JAR" --output "$BUILD_DIR/apk/" @"$BUILD_DIR/classes.txt"

echo "=== 6. Packaging APK ==="
cp "$BUILD_DIR/apk/res.apk" "$BUILD_DIR/apk/unaligned.apk"
cd "$BUILD_DIR/apk"
zip -u -j unaligned.apk classes.dex

# Add assets
mkdir -p "$BUILD_DIR/apk_temp/assets"
cp -r "$PROJECT_DIR/app/src/main/assets/"* "$BUILD_DIR/apk_temp/assets/"
cd "$BUILD_DIR/apk_temp"
zip -u -r "$BUILD_DIR/apk/unaligned.apk" assets/

# Add native libraries (lib/arm64-v8a/libhplip.so)
mkdir -p "$BUILD_DIR/apk_temp/lib/arm64-v8a"
cp "$PROJECT_DIR/app/src/main/lib/arm64-v8a/libhplip.so" "$BUILD_DIR/apk_temp/lib/arm64-v8a/"
zip -u -r "$BUILD_DIR/apk/unaligned.apk" lib/

echo "=== 7. Zipalign ==="
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/apk/unaligned.apk" "$BUILD_DIR/apk/aligned.apk"

echo "=== 8. Signing APK ==="
"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$PROJECT_DIR/DodoPress.apk" \
    "$BUILD_DIR/apk/aligned.apk"

echo "=== Build Complete! Created $PROJECT_DIR/DodoPress.apk ==="
ls -lh "$PROJECT_DIR/DodoPress.apk"
