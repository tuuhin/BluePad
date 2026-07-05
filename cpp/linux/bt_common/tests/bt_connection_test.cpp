#include "bt_common_c.h"
#include <chrono>
#include <gtest/gtest.h>
#include <thread>

TEST(BT_COMMON_TEST, SHOULD_RETURN_TRUE_OR_FALSE) {
    init_logger();
    const bool result = ble_is_bluetooth_active();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_COMMON_TEST, CHECK_IS_PERIPHERIAL_IS_AVALIABLE) {
    const bool result = ble_is_peripheral_role_supported();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_COMMON_TEST, CHECK_IS_SECURE_CONNECTION_AVIALABLR) {
    const bool result = ble_is_secure_connection_available();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_CALLBACK_TEST, REGISTER_CALLBACK) {
    bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener();
}
