cmake_minimum_required(VERSION 4.2)
project(bt_common LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(PkgConfig REQUIRED)
pkg_check_modules(GLIB REQUIRED glib-2.0 gio-2.0)

include(FetchContent)
FetchContent_Declare(
        cpptrace
        GIT_REPOSITORY https://github.com/jeremy-rifkin/cpptrace.git
        GIT_TAG v1.0.4
)

FetchContent_Declare(
        bluez_inc
        GIT_REPOSITORY https://github.com/weliem/bluez_inc
        GIT_TAG main
)

FetchContent_Declare(
        plog
        GIT_REPOSITORY https://github.com/SergiusTheBest/plog
        GIT_TAG 1.1.10
        GIT_SHALLOW true
        SOURCE_SUBDIR cmake_bypass_empty_dir
)

FetchContent_MakeAvailable(cpptrace)
FetchContent_MakeAvailable(plog)
FetchContent_MakeAvailable(bluez_inc)
set_target_properties(Binc PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(bt_common SHARED
        src/bt_common_c.cpp
        src/bt_common_utils.cpp
        src/bt_connection.cpp
        src/bt_bond_manager.cpp
        src/custom_bluez.cpp
)

target_compile_definitions(bt_common PRIVATE BT_COMMON_EXPORTS)

target_include_directories(bt_common
        PUBLIC include
        PRIVATE ${plog_SOURCE_DIR}/include
        PRIVATE ${GLIB_INCLUDE_DIRS}
        PRIVATE ${bluez_inc_SOURCE_DIR}
)

target_link_libraries(bt_common
        PRIVATE Binc
        PRIVATE ${GLIB_LIBRARIES}
        PRIVATE $<$<CONFIG:Debug>:cpptrace::cpptrace>
)

set_target_properties(bt_common PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)

# -- Testing ------------------------------------------------------
FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(bt_common_tests tests/bt_connection_test.cpp)

target_compile_options(bt_common_tests PRIVATE -fsanitize=address -g -O0)
target_link_libraries(bt_common_tests
        PRIVATE bt_common
        PRIVATE GTest::gtest_main
        PRIVATE -fsanitize=address
)
include(GoogleTest)
gtest_discover_tests(bt_common_tests)
