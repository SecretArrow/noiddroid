# ───────────────────────────────────────────────────────────────────────
# Noiddroid native module — links Rust core + Vulkan GPU shim
# ───────────────────────────────────────────────────────────────────────
cmake_minimum_required(VERSION 3.22.1)

project(noiddroid LANGUAGES CXX)

# ── Standard flags ─────────────────────────────────────────────────────
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()
string(TOLOWER "${CMAKE_BUILD_TYPE}" CMAKE_BUILD_TYPE_LOWER)

# ── Rust static library path ────────────────────────────────────────────
# Built by cargo before Gradle assembles the APK. The `rust/build-prepare.sh`
# script cross-compiles to aarch64-linux-android and writes the .a here.
# CMAKE_SOURCE_DIR = app/src/main/cpp/ ; project root is 4 levels up.
set(PROJECT_ROOT "${CMAKE_SOURCE_DIR}/../../../..")
set(RUST_LIB_DIR "${PROJECT_ROOT}/rust/target/aarch64-linux-android/${CMAKE_BUILD_TYPE_LOWER}")
set(RUST_LIB "${RUST_LIB_DIR}/libnoiddroid_core.a")

if(NOT EXISTS "${RUST_LIB}")
    message(WARNING "Rust static lib not found at ${RUST_LIB} — building stub.")
    # Stub library: zero-byte .a created at configure time so link succeeds
    # even before the Rust toolchain runs. Real symbols come from the real .a
    # at build time after `cargo build --target aarch64-linux-android`.
    file(WRITE "${CMAKE_BINARY_DIR}/stub.c" "/* stub */")
    add_library(noiddroid_core STATIC "${CMAKE_BINARY_DIR}/stub.c")
    target_include_directories(noiddroid_core PUBLIC "${CMAKE_SOURCE_DIR}/../rust/src")
else()
    add_library(noiddroid_core STATIC IMPORTED)
    set_target_properties(noiddroid_core PROPERTIES IMPORTED_LOCATION "${RUST_LIB}")
endif()

# ── Vulkan ────────────────────────────────────────────────────────────
find_library(log-lib log)
find_library(android-lib android)

# Vulkan — required for GPU nonce search. Available on Android 7+ (API 24+).
# On minSdk 26 (our setting) it's guaranteed on all shipping arm64 devices.
find_library(vulkan-lib vulkan)

# ── Our shared library ──────────────────────────────────────────────────
add_library(${PROJECT_NAME} SHARED
    jni_bridge.cpp
    vulkan_compute.cpp
)

target_include_directories(${PROJECT_NAME} PRIVATE
    "${CMAKE_SOURCE_DIR}/../rust/src"
)

target_link_libraries(${PROJECT_NAME}
    noiddroid_core
    ${log-lib}
    ${android-lib}
    ${vulkan-lib}
)

# Strip symbols in release for smaller .so
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_link_options(-Wl,--gc-sections -Wl,--strip-all)
endif()
