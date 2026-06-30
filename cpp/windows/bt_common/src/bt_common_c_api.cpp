#include "bt_common_c_api.h"
#include "bluetooth_bond_manager.h"
#include "bluetooth_caller.h"

extern "C" {
bool ble_is_bluetooth_active() {
    try {
        bluetooth_caller caller;
        return caller.is_bluetooth_active().get();
    } catch (...) {
        return false;
    }
}

bool ble_is_secure_connection_available() {
    try {
        return bluetooth_caller::is_ble_secure_connection_available().get();
    } catch (...) {
        return false;
    }
}

bool ble_is_peripheral_role_supported() {
    try {
        return bluetooth_caller::is_peripheral_role_supported().get();
    } catch (...) {
        return false;
    }
}

BluetoothCallerPtr bluetooth_caller_register_listener(BluetoothStatusCallback callback) {
    const auto bt_caller = new bluetooth_caller();

    bt_caller
        ->register_bt_listener([callback](const bool isOn) {
            if (callback) {
                callback(isOn);
            }
        })
        .get();

    return bt_caller;
}

void bluetooth_caller_unregister_listener(const BluetoothCallerPtr caller) {
    auto* bt_caller = static_cast<bluetooth_caller*>(caller);
    if (!bt_caller) return;
    bt_caller->unregister_bt_listener();

    delete bt_caller;
}

BT_COMMON_API bt_bond_state is_device_bonded(const char* device_address) {
    const auto state = bluetooth_bond_manager::get_bond_state(device_address).get();
    return static_cast<bt_bond_state>(state);
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
