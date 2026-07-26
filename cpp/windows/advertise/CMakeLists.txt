cmake_minimum_required(VERSION 4.0.2)
project(ble_advertise LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---------- MSVC Runtime (CRITICAL) ----------
if (MSVC)
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    add_compile_options(/EHsc /bigobj /Zc:__cplusplus /MP)
endif ()

# ---------- Compiler cache ----------
find_program(SCCACHE sccache)
if (SCCACHE)
    message(STATUS "Using sccache: ${SCCACHE}")
    set(CMAKE_CXX_COMPILER_LAUNCHER ${SCCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${SCCACHE})
endif ()

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
        GIT_SHALLOW TRUE
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

# --- googletest: trim gmock/install, force static CRT match ---
set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
set(gtest_force_shared_crt OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_library(ble_advertise SHARED
        src/ble_advertise_c_api.cpp
        src/ble_advertiser.cpp
        src/utils.cpp
)

target_compile_definitions(ble_advertise PRIVATE BT_ADVERTISE_EXPORTS)
target_include_directories(ble_advertise
        PUBLIC include
        PRIVATE ${plog_SOURCE_DIR}/include
)

target_link_libraries(ble_advertise
        PUBLIC windowsapp.lib
        PRIVATE runtimeobject.lib
        PRIVATE $<$<CONFIG:Debug>:cpptrace::cpptrace>
)

set_target_properties(ble_advertise PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)
# Testing
add_executable(ble_advertise_tests tests/tests.cpp)
target_include_directories(ble_advertise_tests PRIVATE ${plog_SOURCE_DIR}/include)

target_link_libraries(ble_advertise_tests
        PRIVATE ble_advertise
        PRIVATE gtest
        PRIVATE gtest_main
)
add_custom_command(TARGET ble_advertise_tests POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:ble_advertise>
        $<TARGET_FILE_DIR:ble_advertise_tests>
)
include(GoogleTest)
gtest_discover_tests(ble_advertise_tests)