cmake_minimum_required(VERSION 3.22)
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

add_library(ble_advertise SHARED src/ble_advertise_c_api.cpp src/ble_advertiser.cpp)
target_compile_definitions(ble_advertise PRIVATE BT_ADVERTISE_EXPORTS)
target_include_directories(ble_advertise PUBLIC include)


target_link_libraries(ble_advertise
        PUBLIC windowsapp.lib
        PRIVATE runtimeobject.lib
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
