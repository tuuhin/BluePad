#include "bt_common_c.h"
#include "bt_bond_manager.h"
#include "bt_common_defination.h"
#include "bt_common_utils.h"
#include "bt_connection.h"

constexpr int timeout_ms = 200L;
extern "C" {

void init_logger() { utils::init_logger(); }

bool ble_is_bluetooth_active() {
    auto response_future = bt_connection::instance().is_bluetooth_active();

    const auto timeout_duration = std::chrono::milliseconds(timeout_ms);
    const auto status           = response_future.wait_for(timeout_duration);

    if (status == std::future_status::ready) {
        return response_future.get();
    }
    if (status == std::future_status::timeout) {
        throw std::runtime_error("Timeout occurred");
    }
    return false;
}

bool ble_is_secure_connection_available() {
    auto response_future = bt_connection::instance().is_ble_secure_connection_available();

    const auto timeout_duration = std::chrono::milliseconds(timeout_ms);
    const auto status           = response_future.wait_for(timeout_duration);

    if (status == std::future_status::ready) {
        return response_future.get();
    }
    if (status == std::future_status::timeout) {
        throw std::runtime_error("Timeout occurred");
    }
    return false;
}

bool ble_is_peripheral_role_supported() {
    auto response_future = bt_connection::instance().is_peripheral_role_supported();

    const auto timeout_duration = std::chrono::milliseconds(timeout_ms);
    const auto status           = response_future.wait_for(timeout_duration);

    if (status == std::future_status::ready) {
        return response_future.get();
    }
    if (status == std::future_status::timeout) {
        throw std::runtime_error("Timeout occurred");
    }
    return false;
}

void bluetooth_caller_register_listener(BluetoothStatusCallback callback) {

    auto& instance = bt_connection::instance();
    instance
        .register_bt_listener([callback](const bool isOn) {
            if (callback == nullptr) return;
            callback(isOn);
        })
        .get();
}

void bluetooth_caller_unregister_listener() {
    auto& instance = bt_connection::instance();
    instance.unregister_bt_listener();
}

BT_COMMON_API bt_bond_state is_device_bonded(const char* device_address) {

    const auto& instance = bluetooth_bond_manager::instance();
    auto response_future = instance.get_bond_state(device_address);

    const auto timeout_duration = std::chrono::milliseconds(timeout_ms);
    const auto status           = response_future.wait_for(timeout_duration);

    if (status == std::future_status::ready) {
        return response_future.get();
    }
    if (status == std::future_status::timeout) {
        throw std::runtime_error("Timeout occurred");
    }
    return ERROR_UNKNOWN;
}

BT_COMMON_API bt_bond_manager_handle create_bond_manager() {
    return new std::shared_ptr(std::make_shared<bluetooth_bond_manager>());
}

BT_COMMON_API void destroy_bond_manager(const bt_bond_manager_handle handle) {
    delete static_cast<std::shared_ptr<bluetooth_bond_manager>*>(handle);
}

BT_COMMON_API void bond_manager_request_pairing(const bt_bond_manager_handle handle, const char* device_address,
                                                bt_bond_pairing_callback callback) {
    const auto* manager_ptr = static_cast<std::shared_ptr<bluetooth_bond_manager>*>(handle);
    if (!manager_ptr || !*manager_ptr) return;

    const std::string addr(device_address);
    (*manager_ptr)->request_and_register_bond_callback(addr, callback);
}

BT_COMMON_API void bond_manager_unregister_pairing(const bt_bond_manager_handle handle) {
    const auto* manager_ptr = static_cast<std::shared_ptr<bluetooth_bond_manager>*>(handle);
    if (!manager_ptr || !*manager_ptr) return;
    (*manager_ptr)->unregister_bond_callback();
}

BT_COMMON_API void bond_manager_accept_connection(const bt_bond_manager_handle handle, const char* pin,
                                                  const bt_bond_responder_handle responder) {
    const auto* manager_ptr = static_cast<std::shared_ptr<bluetooth_bond_manager>*>(handle);
    if (!manager_ptr || !*manager_ptr) return;
    (*manager_ptr)->accept_connection(pin, responder);
}
}

void bond_manager_reject_connection(const bt_bond_manager_handle handle, const bt_bond_responder_handle responder) {
    const auto* manager_ptr = static_cast<std::shared_ptr<bluetooth_bond_manager>*>(handle);
    if (!manager_ptr || !*manager_ptr) return;
    (*manager_ptr)->cancel_connection(responder);
}
