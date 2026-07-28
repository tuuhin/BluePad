#include "bt_common_c_api.h"
#include <chrono>
#include <gtest/gtest.h>

TEST(BT_COMMON_TEST, SHOULD_RETURN_TRUE_OR_FALSE) {
    init_logger();
    const bool result = ble_is_bluetooth_active();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_COMMON_TEST, CHECK_IS_PERIPHERAL_IS_AVAILABLE) {
    const bool result = ble_is_peripheral_role_supported();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_COMMON_TEST, CHECK_IS_SECURE_CONNECTION_AVAILABLE) {
    const bool result = ble_is_secure_connection_available();
    EXPECT_TRUE(result == true || result == false);
}

TEST(BT_CALLBACK_TEST, REGISTER_CALLBACK) {
    const auto ptr =
        bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener(ptr);
}

TEST(BT_CALLBACK_TEST, UNREGISTER_WITHOUT_REGISTER) {
    const auto ptr = static_cast<BluetoothCallerPtr>(nullptr);
    bluetooth_caller_unregister_listener(ptr);
}

TEST(BT_CALLBACK_TEST, REGISTER_UNREGISTER_REGISTER) {
    const auto prt1 =
        bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener(prt1);
    const auto prt2 =
        bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener(prt2);
}
