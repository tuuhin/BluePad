#include "bt_common_c_api.h"
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
        bluetooth_caller caller;
        return caller.is_ble_secure_connection_available().get();
    } catch (...) {
        return false;
    }
}

bool ble_is_peripheral_role_supported() {
    try {
        bluetooth_caller caller;
        return caller.is_peripheral_role_supported().get();
    } catch (...) {
        return false;
    }
}

BluetoothCallerPtr bluetooth_caller_register_listener(BluetoothStatusCallback callback) {
    const auto bt_caller = new bluetooth_caller();

    bt_caller
        ->register_bt_listener([callback](bool isOn) {
            if (callback) {
                callback(isOn);
            }
        })
        .get();

    return bt_caller;
}

void bluetooth_caller_unregister_listener(BluetoothCallerPtr caller) {
    auto* bt_caller = static_cast<bluetooth_caller*>(caller);
    if (!bt_caller)
        return;
    bt_caller->unregister_bt_listener();

    delete bt_caller;
}
}
