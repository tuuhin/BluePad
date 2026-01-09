project(bt_common LANGUAGES CXX)

add_library(bt_common SHARED src/bt_common.cpp)

target_include_directories(bt_common PRIVATE
        include
        ${JNI_INCLUDE_DIRS}
)
target_link_libraries(bt_common PRIVATE
        ${JNI_LIBRARIES}
        windowsapp.lib
)

target_link_libraries(bt_common PRIVATE
        ${JNI_LIBRARIES}
        windowsapp.lib
)

target_link_libraries(bt_common PRIVATE
        ${JNI_LIBRARIES}
        windowsapp.lib
)

# ---------- Native Test EXE ----------
add_executable(bt_common_test tests/test.cpp)
target_include_directories(bt_common_test PRIVATE ${JNI_INCLUDE_DIRS} include)
target_link_libraries(bt_common_test PRIVATE windowsapp.lib)

enable_testing()
add_test(NAME BT_COMMON_TESTS COMMAND bt_common_test)
