cmake_minimum_required(VERSION 4.0.2)
project(bt_common LANGUAGES CXX)

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

FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
        GIT_SHALLOW TRUE
        GIT_PROGRESS FALSE
)

FetchContent_MakeAvailable(cpptrace)
FetchContent_MakeAvailable(plog)

set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
set(gtest_force_shared_crt OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

# ---------- DLL instead of STATIC ----------
add_library(bt_common SHARED
        src/bluetooth_caller.cpp
        src/bt_common_c_api.cpp
        src/bluetooth_bond_manager.cpp
        src/utils.cpp
)

# Triggers __declspec(dllexport) in your header when building the DLL
target_compile_definitions(bt_common PRIVATE BT_COMMON_EXPORTS)

target_include_directories(bt_common
        PUBLIC include
        PRIVATE ${plog_SOURCE_DIR}/include
)

target_link_libraries(bt_common
        PUBLIC windowsapp.lib
        PRIVATE runtimeobject.lib
        PRIVATE $<$<CONFIG:Debug>:cpptrace::cpptrace>
)

# Control where .dll and .lib are output
set_target_properties(bt_common PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin   # .dll goes here
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib   # .lib goes here
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)

add_executable(bt_common_tests tests/test.cpp)

target_link_libraries(bt_common_tests
        PRIVATE bt_common
        PRIVATE gtest
        PRIVATE gtest_main
)

add_custom_command(TARGET bt_common_tests POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:bt_common>
        $<TARGET_FILE_DIR:bt_common_tests>
        COMMENT "Copying bt_common.dll to test output directory for discovery execution"
)
include(GoogleTest)
gtest_discover_tests(bt_common_tests)
