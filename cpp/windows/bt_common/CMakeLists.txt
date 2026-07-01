cmake_minimum_required(VERSION 3.22)
project(bt_common LANGUAGES CXX)

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

# stack trace to show what
include(FetchContent)
FetchContent_Declare(
        cpptrace
        GIT_REPOSITORY https://github.com/jeremy-rifkin/cpptrace.git
        GIT_TAG v1.0.4
)
FetchContent_MakeAvailable(cpptrace)

# ---------- DLL instead of STATIC ----------
add_library(bt_common SHARED
        src/bluetooth_caller.cpp
        src/bt_common_c_api.cpp
        src/bluetooth_bond_manager.cpp
        src/utils.cpp
)

# Triggers __declspec(dllexport) in your header when building the DLL
target_compile_definitions(bt_common PRIVATE BT_COMMON_EXPORTS)

target_include_directories(bt_common PUBLIC include)

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
        WINDOWS_EXPORT_ALL_SYMBOLS OFF  # we use __declspec manually
)

# ---------- Native Test EXE ----------
add_executable(bt_common_test tests/test.cpp)
target_include_directories(bt_common_test PRIVATE include)
target_link_libraries(bt_common_test PRIVATE bt_common)

# Copy .dll next to test .exe so it can run
add_custom_command(TARGET bt_common_test POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:bt_common>
        $<TARGET_FILE_DIR:bt_common_test>
)

enable_testing()
add_test(NAME BT_COMMON_TESTS COMMAND bt_common_test)