cmake_minimum_required(VERSION 4.0.2)

if (POLICY CMP0194)
    cmake_policy(SET CMP0194 NEW)
endif ()

project(native_compression LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---------- MSVC Runtime ----------
if (MSVC)
    # /MT = static CRT
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    add_compile_options(/EHsc /bigobj /Zc:__cplusplus /MP)
endif ()

find_program(SCCACHE sccache)
if (SCCACHE)
    message(STATUS "USING SCCACHE: ${SCCACHE}")
    set(CMAKE_CXX_COMPILER_LAUNCHER ${SCCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${SCCACHE})
endif ()

include(FetchContent)

# 1. Fetch zlib (gzip & deflate)
FetchContent_Declare(
        zlib
        GIT_REPOSITORY https://github.com/madler/zlib.git
        GIT_TAG v1.3.1
)

FetchContent_Declare(
        lz4
        GIT_REPOSITORY https://github.com/lz4/lz4.git
        GIT_TAG v1.10.0
        SOURCE_SUBDIR build/cmake
)

FetchContent_Declare(
        zstd
        GIT_REPOSITORY https://github.com/facebook/zstd.git
        GIT_TAG v1.5.7
        SOURCE_SUBDIR build/cmake
)


# Set dependency options BEFORE calling FetchContent_MakeAvailable
set(ZLIB_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LZ4_BUILD_CLI OFF CACHE BOOL "" FORCE)
set(LZ4_BUILD_LEGACY_LZ4C OFF CACHE BOOL "" FORCE)
set(ZSTD_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(ZSTD_BUILD_TESTS OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(zlib lz4 zstd)

add_library(native_compression SHARED src/native_compression_manager.cpp src/native_compression_c_api.cpp)

target_compile_definitions(native_compression PRIVATE ALLOW_native_compression_MODULE_EXPORTS)

target_include_directories(native_compression PUBLIC include)

target_link_libraries(native_compression
        PUBLIC windowsapp.lib
        PRIVATE
        runtimeobject.lib
        zlibstatic
        lz4_static
        libzstd_static
)

# Output directories for binaries
set_target_properties(native_compression PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)
