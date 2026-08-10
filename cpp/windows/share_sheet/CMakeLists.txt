cmake_minimum_required(VERSION 4.0.2)
project(share_sheet LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---------- MSVC Runtime (CRITICAL) ----------
if (MSVC)
    # /MT = static CRT — embeds MSVC runtime inside the DLL
    # so lld (Kotlin Native) never has to resolve libcpmt.a etc.
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    add_compile_options(/EHsc /bigobj /Zc:__cplusplus /MP)
endif ()

find_program(SCCACHE sccache)
if (SCCACHE)
    message(STATUS "USING SCCACHE: ${SCCACHE}")
    set(CMAKE_CXX_COMPILER_LAUNCHER ${SCCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${SCCACHE})
endif ()

# stack trace to show what
include(FetchContent)
FetchContent_Declare(
        cpptrace
        GIT_REPOSITORY https://github.com/jeremy-rifkin/cpptrace.git
        GIT_TAG v1.0.4
        GIT_SHALLOW TRUE
        GIT_PROGRESS FALSE
)

FetchContent_Declare(
        plog
        GIT_REPOSITORY https://github.com/SergiusTheBest/plog
        GIT_TAG 1.1.10
        GIT_SHALLOW true
        GIT_PROGRESS FALSE
        SOURCE_SUBDIR cmake_bypass_empty_dir
)

FetchContent_MakeAvailable(cpptrace)
FetchContent_MakeAvailable(plog)

# ---------- DLL instead of STATIC ----------
add_library(share_sheet SHARED
        src/share_sheet_c.cpp
        src/share_sheet.cpp
        src/utils.cpp
)

# Triggers __declspec(dllexport) in your header when building the DLL
target_compile_definitions(share_sheet PRIVATE SHARE_SHEET_EXPORTS)

target_include_directories(share_sheet
        PUBLIC include
        PRIVATE ${plog_SOURCE_DIR}/include
)

target_link_libraries(share_sheet
        PUBLIC windowsapp.lib
        PRIVATE runtimeobject.lib
        PRIVATE $<$<CONFIG:Debug>:cpptrace::cpptrace>
)

# Control where .dll and .lib are output
set_target_properties(share_sheet PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin   # .dll goes here
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib   # .lib goes here
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)
