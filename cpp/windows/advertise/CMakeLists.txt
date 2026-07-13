cmake_minimum_required(VERSION 4.0.2)
project(ble_advertise LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---------- MSVC Runtime (CRITICAL) ----------
if (MSVC)
    # /MT = static CRT — embeds MSVC runtime inside the DLL
    # so lld (Kotlin Native) never has to resolve libcpmt.a etc.
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    add_compile_options(/EHsc /bigobj /Zc:__cplusplus)
endif ()


include(FetchContent)
FetchContent_Declare(
        cpptrace
        GIT_REPOSITORY https://github.com/jeremy-rifkin/cpptrace.git
        GIT_TAG v1.0.4
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

# Control where .dll and .lib are output
set_target_properties(ble_advertise PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin   # .dll goes here
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib   # .lib goes here
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        WINDOWS_EXPORT_ALL_SYMBOLS OFF  # we use __declspec manually
)

# ---------- Native Test EXE ----------
add_executable(ble_advertise_test tests/tests.cpp)
target_include_directories(ble_advertise_test PRIVATE include)
target_link_libraries(ble_advertise_test PRIVATE ble_advertise)

add_custom_command(TARGET ble_advertise_test POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:ble_advertise>
        $<TARGET_FILE_DIR:ble_advertise_test>
)

enable_testing()
add_test(NAME ADVERTISEMENT_TEST COMMAND ble_advertise_test)