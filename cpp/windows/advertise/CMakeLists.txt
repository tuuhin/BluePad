
project(ble_java_advertise LANGUAGES CXX)

add_library(ble_java_advertise SHARED src/library.cpp)

target_include_directories(ble_java_advertise PRIVATE
        include
        ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ble_java_advertise PRIVATE
        ${JNI_LIBRARIES}
        windowsapp.lib
)


# ---------- Native Test EXE ----------
add_executable(ble_advertise_test tests/tests.cpp)

target_include_directories(ble_advertise_test PRIVATE
        ${JNI_INCLUDE_DIRS}
        include
)

target_link_libraries(ble_advertise_test PRIVATE
        windowsapp.lib
)

enable_testing()
add_test(NAME ADVERTISEMENT_TEST COMMAND ble_advertise_test)
