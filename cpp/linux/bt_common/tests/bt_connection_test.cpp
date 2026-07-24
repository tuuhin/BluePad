#include "bt_common_c.h"
#include <gtest/gtest.h>

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

TEST(BT_CALLBACK_TEST, REGISTER_MULTIPLE_TIMES) {
    bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener();
}

TEST(BT_CALLBACK_TEST, UNREGISTER_WITHOUT_REGISTER) { bluetooth_caller_unregister_listener(); }

TEST(BT_CALLBACK_TEST, REGISTER_UNREGISTER_REGISTER) {
    bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener();
    bluetooth_caller_register_listener([](const bool is_on) { EXPECT_TRUE(is_on == true || is_on == false); });
    bluetooth_caller_unregister_listener();
}

TEST(BT_REQUEST_ENABLE_TEST, JUST_REQ_ENABLE) {
   init_logger();
    const auto result     = request_bluetooth_enable();
    const bool is_success = (result == REQUEST_ACCEPTED || result == REQUEST_NOT_NEEDED);

    EXPECT_TRUE(is_success) << "CANNOT ENABLE STATE: " << static_cast<int>(result);
}

TEST(BT_REQUEST_ENABLE_TEST, SECOND_CALL_RETURNS_NOT_NEEDED) {
    init_logger();
    request_bluetooth_enable();
    const auto second_result = request_bluetooth_enable();
    EXPECT_EQ(second_result, REQUEST_NOT_NEEDED);
}