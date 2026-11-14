#!/usr/bin/env bash
# Build script for libpantum.so (ARM64 Android Bionic)
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="$DIR/../../lib/arm64-v8a"
ASSETS_DIR="$DIR/../../assets"

mkdir -p "$TARGET_DIR"
mkdir -p "$ASSETS_DIR"

echo "Compiling libpantum.so..."
clang -O3 -shared -fPIC -target aarch64-linux-android24 \
    -I"$DIR" \
    "$DIR/pantum_filter.c" \
    "$DIR/jbig.c" \
    "$DIR/jbig_ar.c" \
    -o "$TARGET_DIR/libpantum.so"

echo "Stripping DT_RUNPATH from ELF dynamic table..."
python3 -c "
import struct

path = '$TARGET_DIR/libpantum.so'
with open(path, 'r+b') as f:
    data = bytearray(f.read())
    shoff = struct.unpack_from('<Q', data, 40)[0]
    shentsize = struct.unpack_from('<H', data, 58)[0]
    shnum = struct.unpack_from('<H', data, 60)[0]

    dyn_offset = None
    dyn_size = None
    for i in range(shnum):
        sec = shoff + i * shentsize
        sh_type = struct.unpack_from('<I', data, sec + 4)[0]
        if sh_type == 6:
            dyn_offset = struct.unpack_from('<Q', data, sec + 24)[0]
            dyn_size = struct.unpack_from('<Q', data, sec + 32)[0]
            break

    assert dyn_offset is not None
    entries = []
    off = dyn_offset
    while off < dyn_offset + dyn_size:
        tag, val = struct.unpack_from('<QQ', data, off)
        entries.append((tag, val))
        off += 16

    new_entries = [(t, v) for (t, v) in entries if t not in (0x1d, 0x0f, 0x00)]
    while len(new_entries) * 16 < dyn_size:
        new_entries.append((0, 0))

    for i, (t, v) in enumerate(new_entries):
        struct.pack_into('<QQ', data, dyn_offset + i * 16, t, v)

    f.seek(0)
    f.write(data)
"

cp "$TARGET_DIR/libpantum.so" "$ASSETS_DIR/libpantum.so"
echo "Built and installed libpantum.so successfully!"
