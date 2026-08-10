cmake_minimum_required(VERSION 4.2)
project(ble_advertise LANGUAGES CXX C)

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

add_library(ble_advertise SHARED
        src/bt_utils.cpp
        src/ble_advertiser.cpp
        src/ble_advertise_c.cpp
)

target_compile_definitions(ble_advertise PRIVATE ble_advertise_EXPORTS)

target_include_directories(ble_advertise
        PUBLIC include
        PRIVATE ${plog_SOURCE_DIR}/include
        PRIVATE ${GLIB_INCLUDE_DIRS}
        PRIVATE ${bluez_inc_SOURCE_DIR}
)

target_link_libraries(ble_advertise
        PRIVATE Binc
        PRIVATE ${GLIB_LIBRARIES}
        PRIVATE $<$<CONFIG:Debug>:cpptrace::cpptrace>
)

set_target_properties(ble_advertise PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF
)

# -- Testing ------------------------------------------------------
enable_testing()

FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(ble_advertise_tests tests/ble_tests.cpp)

target_compile_options(ble_advertise_tests PRIVATE -fsanitize=address -g -O0)
target_link_options(ble_advertise_tests PRIVATE -fsanitize=address)
target_link_libraries(ble_advertise_tests
        PRIVATE ble_advertise
        PRIVATE GTest::gtest_main
)
set_target_properties(ble_advertise_tests PROPERTIES
        BUILD_RPATH "${CMAKE_BINARY_DIR}/lib"
)
include(GoogleTest)
if (NOT CMAKE_CROSSCOMPILING)
    gtest_discover_tests(ble_advertise_tests)
endif ()
