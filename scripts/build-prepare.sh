#!/usr/bin/env bash
# rust/build-prepare.sh — cross-compile the Rust core to aarch64-linux-android
# and copy the static lib into the location CMake expects.
#
# Run by GitHub Actions BEFORE the Gradle assemble step. Locally, can be
# invoked manually for testing the native pipeline.
#
# Requires:
#   * rustup toolchain manager
#   * Android NDK r26+ (ANDROID_NDK_HOME set)
#   * `rustup target add aarch64-linux-android`
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUST_DIR="$PROJECT_ROOT/rust"

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  echo "ERROR: ANDROID_NDK_HOME not set" >&2
  exit 1
fi

echo "==> Building Rust core for aarch64-linux-android (release profile)"

# Use the NDK-bundled clang linker so we don't need to install a separate
# standalone toolchain.
export CC_aarch64_linux_android="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang"
export CXX_aarch64_linux_android="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
export AR_aarch64_linux_android="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$CC_aarch64_linux_android"

(
  cd "$RUST_DIR"
  cargo build --target aarch64-linux-android --release --lib
  # Also build a debug variant so the debug APK can link too
  cargo build --target aarch64-linux-android --lib
)

# Verify artifacts exist where CMakeLists.txt expects them.
# (No copy needed — cargo already writes to the correct path.)
RELEASE_LIB="$RUST_DIR/target/aarch64-linux-android/release/libnoiddroid_core.a"
DEBUG_LIB="$RUST_DIR/target/aarch64-linux-android/debug/libnoiddroid_core.a"

if [[ ! -f "$RELEASE_LIB" ]]; then
  echo "ERROR: release artifact missing at $RELEASE_LIB" >&2
  exit 1
fi
if [[ ! -f "$DEBUG_LIB" ]]; then
  echo "ERROR: debug artifact missing at $DEBUG_LIB" >&2
  exit 1
fi

echo "==> Rust build complete:"
ls -lh "$RELEASE_LIB" "$DEBUG_LIB"
