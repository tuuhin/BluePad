#include <chrono>
#include <gtest/gtest.h>
#include <thread>

#include "ble_advertise_c_api.h"

struct TestContext {
    std::atomic<bool> service_added_successfully{false};
    std::atomic<bool> service_added_failed{false};
    std::atomic<bool> advertising_started{false};
    int32_t last_error = 0;
};

static void OnServiceAdded(const char* uuid, int32_t error_code, void* user_data) {
    auto* ctx = static_cast<TestContext*>(user_data);
    ctx->last_error = error_code;
    if (error_code == 0)
        ctx->service_added_successfully = true;
    else
        ctx->service_added_failed = true;
}

static void OnServiceStatusChange(int32_t status, void* user_data) {
    auto* ctx = static_cast<TestContext*>(user_data);
    if (status == 1) ctx->advertising_started = true;
}

TEST(BLE_ADVERTISE_RUN_TEST, AdvertiseForTenSeconds) {
    TestContext context;
    const auto advertiser = ble_advertiser_create();
    ASSERT_NE(advertiser, nullptr) << "Failed to allocate the native BLE Advertiser object pointer.";

    BLEAdvertiserCallbacks callbacks = {};
    std::memset(&callbacks, 0, sizeof(BLEAdvertiserCallbacks));
    callbacks.user_data                = &context;
    callbacks.on_service_added         = OnServiceAdded;
    callbacks.on_service_status_change = OnServiceStatusChange;
    callbacks.on_read_characteristic   = nullptr;
    callbacks.on_write_characteristic  = nullptr;
    callbacks.on_read_descriptor       = nullptr;
    callbacks.on_write_descriptor      = nullptr;
    callbacks.on_indication_result     = nullptr;

    ble_advertiser_register_callbacks(advertiser, callbacks);

    const auto target_service_uuid = "11223344-5566-7788-99AA-BBCCDDEEFF00";
    ble_advertiser_add_service(advertiser, target_service_uuid);

    const auto start_time = std::chrono::steady_clock::now();
    while (!context.service_added_successfully && !context.service_added_failed) {
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        if (std::chrono::steady_clock::now() - start_time > std::chrono::seconds(3)) {
            FAIL() << "Timeout waiting for WinRT to initialize the GattServiceProvider.";
        }
    }

    ASSERT_FALSE(context.service_added_failed)
        << "GattServiceProvider initialization failed with code: " << context.last_error;

    ble_characteristics test_char = {};
    std::memset(&test_char, 0, sizeof(ble_characteristics));
    test_char.characteristic_uuid = "AABBCCDD-EEFF-1122-3344-556677889900";
    test_char.can_read            = true;
    test_char.can_write           = true;

    ble_advertiser_add_characteristic(advertiser, test_char);

    const auto hello_payload  = "Hello";
    BLEAdvertiseConfig config = {};
    config.discoverable       = false;
    config.connectable        = true;
    config.service_data       = reinterpret_cast<const uint8_t*>(hello_payload);
    config.service_data_len   = std::strlen(hello_payload);

    ble_advertiser_start(advertiser, config);
    std::this_thread::sleep_for(std::chrono::milliseconds(2000));
    ble_advertiser_stop(advertiser);
    ble_advertiser_destroy(advertiser);
}

TEST(BLE_ADVERTISE_RUN_TEST, DestroyWithNull) {
    const auto advertiser = static_cast<BLEAdvertiserPtr>(nullptr);
    ble_advertiser_destroy(advertiser);
}

TEST(BLE_ADVERTISE_RUN_TEST, CreateDestroyLifecycleBasic) {
    const auto advertiser = ble_advertiser_create();
    ASSERT_NE(advertiser, nullptr);
    ble_advertiser_destroy(advertiser);
}

TEST(BLE_ADVERTISE_RUN_TEST, NullPointerResilience) {
    const auto advertiser = static_cast<BLEAdvertiserPtr>(nullptr);

    // Call functions with null advertiser
    ble_advertiser_register_callbacks(advertiser, {});
    EXPECT_EQ(ble_advertiser_get_status(advertiser), -1);
    ble_advertiser_start(advertiser, {});
    ble_advertiser_stop(advertiser);
    ble_advertiser_add_service(advertiser, nullptr);
    ble_advertiser_add_characteristic(advertiser, {});
    ble_advertiser_add_descriptor(advertiser, nullptr, nullptr);
    EXPECT_FALSE(ble_advertiser_send_notification(advertiser, nullptr, nullptr, nullptr, 0));
}