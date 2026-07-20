# Windows Library Build

This project uses CMake to build C++ modules for Windows functionality, specifically related to Bluetooth Low Energy (
BLE) advertising and common utilities. The build system is designed to produce shared libraries (`.dll`) with pure C
interfaces so that it can be directly linked with KMP native.

## Prerequisites

- **CMake:** Version 4.0.2 or higher.
- **Compiler:** Visual Studio 2022 (MSVC).
- **Windows SDK:** Required for system libraries like `windowsapp.lib` and `runtimeobject.lib`.

## Build Configuration

- **Standard:** C++20.
- **Runtime:** Static MultiThreaded runtime (`/MT` for Release, `/MTd` for Debug). This is critical to ensure the DLLs
  are self-contained and don't introduce dependencies on external MSVC runtime DLLs, which avoids conflicts with Kotlin
  Native.
- **Compiler Options:** `/EHsc /bigobj /Zc:__cplusplus /MP`
- **Compiler Cache:** The build system attempts to use `sccache` if it's found in the system path to speed up
  compilation.

## Dependencies (Fetched via FetchContent)

The build process automatically downloads the following dependencies:

- **[cpptrace](https://github.com/jeremy-rifkin/cpptrace):** Used for stack trace generation in Debug builds.
- **[plog](https://github.com/SergiusTheBest/plog):** A lightweight logging library.
- **[googletest](https://github.com/google/googletest):** Used for unit testing the C++ components.

## Modules

The project consists of two primary shared library modules:

1. **`ble_advertise`**: Handles Windows BLE advertisement.
2. **`bt_common`**: Provides shared Bluetooth utilities and bond management.

Both modules are exported as shared libraries (`SHARED`).

## Build Output

- **Binaries (`.dll`):** Output to `CMAKE_BINARY_DIR/bin`.
- **Libraries (`.lib`):** Output to `CMAKE_BINARY_DIR/lib`.
