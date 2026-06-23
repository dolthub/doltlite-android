#!/bin/bash
#
# Cross-compile libdoltlite for the Android ABIs from a doltlite source checkout
# and lay the results out as src/main/jniLibs/<abi>/libdoltlite.so for the AAR.
# Each ABI compiles the doltlite amalgamation (sqlite3.c, with the dolt_*
# functions built in and auto-registered) as a single shared object.
#
# Requires the Android NDK and a doltlite checkout whose amalgamation has been
# generated (`./configure && make sqlite3.c sqlite3.h`).
#
# Usage: build-libs.sh <doltlite_src_dir> <version> [ndk_dir] [min_api]

set -euo pipefail

SRC="${1:?usage: build-libs.sh <doltlite_src_dir> <version> [ndk_dir] [min_api]}"
VERSION="${2:?usage: build-libs.sh <doltlite_src_dir> <version> [ndk_dir] [min_api]}"
NDK="${3:-${ANDROID_NDK_HOME:-}}"
MIN_API="${4:-21}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
  echo "ERROR: Android NDK not found (set ANDROID_NDK_HOME or pass it as arg 3)" >&2
  exit 1
fi
if [ ! -f "$SRC/sqlite3.c" ] || [ ! -f "$SRC/sqlite3.h" ]; then
  echo "ERROR: $SRC/sqlite3.c not found; run './configure && make sqlite3.c sqlite3.h' in the doltlite checkout" >&2
  exit 1
fi

case "$(uname -s)" in
  Darwin) HOST_TAG="darwin-x86_64" ;;
  *)      HOST_TAG="linux-x86_64" ;;
esac
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
if [ ! -d "$TOOLCHAIN" ]; then
  echo "ERROR: NDK toolchain not found at $TOOLCHAIN" >&2
  exit 1
fi

OUT="$ROOT/src/main/jniLibs"
rm -rf "$OUT"

CFLAGS_COMMON=(
  -O2 -fPIC -DNDEBUG
  -DSQLITE_ENABLE_MATH_FUNCTIONS -DSQLITE_THREADSAFE=1
  -DDOLTLITE_PROLLY=1 -DDOLTLITE_VERSION="\"v${VERSION}\""
  -DSQLITE_ENABLE_FTS5 -DSQLITE_ENABLE_RTREE -DSQLITE_ENABLE_DBSTAT_VTAB
  -DSQLITE_HAVE_ZLIB=1
  -Wno-comment
)

# abi : clang target triple prefix (the NDK clang wrapper is <triple><api>-clang)
ABIS=(
  "arm64-v8a:aarch64-linux-android"
  "armeabi-v7a:armv7a-linux-androideabi"
  "x86_64:x86_64-linux-android"
  "x86:i686-linux-android"
)

for entry in "${ABIS[@]}"; do
  IFS=: read -r abi triple <<< "$entry"
  cc="$TOOLCHAIN/bin/${triple}${MIN_API}-clang"
  if [ ! -x "$cc" ]; then
    echo "ERROR: compiler not found for $abi: $cc" >&2
    exit 1
  fi
  mkdir -p "$OUT/$abi"
  # zlib (libz) ships in the NDK sysroot.
  "$cc" "${CFLAGS_COMMON[@]}" -shared "$SRC/sqlite3.c" -lz -o "$OUT/$abi/libdoltlite.so"
  echo "built $abi -> $OUT/$abi/libdoltlite.so"
done

echo "jniLibs:"
find "$OUT" -name '*.so' | sort
