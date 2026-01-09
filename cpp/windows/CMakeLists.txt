cmake_minimum_required(VERSION 4.1.2)
project(windows_ble LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---------- MSVC Runtime (CRITICAL) ----------
if (MSVC)
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    add_compile_options(/EHsc /bigobj /Zc:__cplusplus)
endif ()

# ---------- JNI ----------
find_package(JNI REQUIRED)

add_subdirectory(advertise)
add_subdirectory(bt_common)
