# Windows Native Build Architecture

This document outlines the architecture for building and integrating native Windows modules (`bt_common` and
`ble_advertise`) into the BluePad Kotlin Multiplatform project.

## The Compiler ABI Challenge

We face a fundamental binary incompatibility when building for Windows:

- **Kotlin/Native** targets the `mingwX64` environment (GCC-based ABI).
- **Advanced Windows Bluetooth/Native APIs** are primarily supported or easiest to interface with using MSVC (`cl.exe`,
  Microsoft's compiler).

**The Problem:** MinGW (GCC) and MSVC have incompatible binary interfaces (ABIs) for C++ (different name mangling, STL
implementations, and exception handling). They cannot link C++ objects directly.

**The Solution: Pure C ABI Boundary**
To bridge this, we enforce a strict **`extern "C"`** boundary.

- **C++ Layer (MSVC):** Implements the logic and exposes a C-style API (`extern "C"`) in a DLL.
- **Kotlin Layer (MinGW):** Calls into the DLL via CInterop.

By using a pure C ABI, we bypass the C++ incompatibilities.

## Static Runtime Strategy

To avoid dependencies on specific Microsoft CRT (C Runtime) versions (which are not guaranteed to be installed on all
Windows machines), we configure the MSVC build to use the **Static Runtime**:

```cmake
# CMake configuration
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
```

This statically links the required runtime libraries into our `bt_common.dll` and `ble_advertise.dll`, ensuring they
only depend on the fundamental `MSVCRT.dll` present on all Windows systems.

## Module Build Flow

Both `bt_common` and `ble_advertise` follow a similar build process orchestrated by Gradle:

1. **CMake Build:** Gradle triggers CMake, which invokes `cl.exe` (MSVC) to compile the C++ source files.
    - CMake outputs: `[ModuleName].dll` and `[ModuleName].lib` (the import library).
2. **Kotlin/Native Link:** Kotlin/Native (MinGW) compiles the Kotlin wrapper code.
    - The Kotlin linker consumes the `.lib` file to map API symbols to the DLL.
3. **DLL Bundling (KNE/Gradle):**
    - Windows loader requires the DLL to be discoverable at runtime.
    - Gradle copy tasks place the compiled DLLs side-by-side with the generated Kotlin/Native binary.
    - For JVM JAR distribution, the Nucleus Native Access (KNE) plugin bundles these DLLs into the final JAR for
      automatic extraction and loading.

## Module Relationships

- **`bt_common`**: Provides the foundational Bluetooth structures and platform-specific info providers.
- **`ble_advertise`**: Depends on `bt_common`. It provides higher-level advertisement and characteristic management.

### Linker Requirements

Because `ble_advertise` depends on `bt_common`, the linker must be able to resolve symbols from *both* modules.

- The `ble-advertise` Gradle configuration must correctly include the search paths for `bt_common` import libraries (
  `.lib`) in its `linkerOpts` to satisfy the linker dependencies.
- At runtime, *both* `ble_advertise.dll` and `bt_common.dll` must be in the `PATH` or the executable's directory for the
  Windows loader to satisfy dependencies.

## Gradle Build Configuration Specifics

Our `build.gradle.kts` files contain specific logic to manage the MSVC/MinGW impedance mismatch.

### 1. Linker Configuration (`linkerOpts`)

When Kotlin/Native compiles and links, it needs the MSVC-generated `.lib` import library to know where the symbols are
in the DLL.

- **Why we search both Debug/Release:** The build can be invoked for different build types. To ensure stability, we
  explicitly provide search paths (`-L...`) for both configurations so the linker can find the correct import library
  regardless of the current build type.

### 2. DLL Copy Tasks

Windows requires the DLL to be present next to the executable (or in the system/search path).

- **`copy[Module]DllToLinkDir...`**: This task ensures the C++ DLL is copied beside the generated Kotlin/Native binary.
  This is required by the Windows loader when the Kotlin/Native test executable runs (as it depends on the C++ DLL).
- **`copy[Module]DllToKne`**: This task copies the C++ DLL into the `generated/kne/nativeLib/...` folder. The
  `nucleus-nna` plugin then picks up these files from this specific directory to bundle them into the final JVM JAR,
  ensuring the DLL is available when the JVM app runs.

### 3. PATH Environment Variable (`setupNativePath`)

Unlike Linux/macOS, Windows *does not* automatically search the folder of the loading DLL for its dependencies (unless
specifically configured).

- **The Issue:** When our JVM test runs and loads `bleAdvertiseNative.dll` (via FFM/KNE), the Windows loader looks for
  `bt_common.dll` in the current working directory and the system PATH—**but not** in the `mingwX64` binary folder where
  our native dependencies actually live.
- **The Fix:** We inject the path to the `mingwX64` binary directories into the `PATH` environment variable for all
  `Test` and `JavaExec` tasks via the `setupNativePath` helper function in `build.gradle.kts`. This guarantees that when
  the JVM loads the primary native library, the Windows loader can successfully resolve the entire dependency tree.
