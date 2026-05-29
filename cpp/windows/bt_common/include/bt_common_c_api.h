#ifndef BT_COMMON_C_API_H
#define BT_COMMON_C_API_H

#include <stdbool.h>
#include <stdint.h>

#ifdef _MSC_VER
#ifdef BT_COMMON_EXPORTS
#define BT_COMMON_API __declspec(dllexport)
#else
#define BT_COMMON_API __declspec(dllimport)
#endif
#else
#define BT_COMMON_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

// Global utility functions
BT_COMMON_API bool ble_is_bluetooth_active();
BT_COMMON_API bool ble_is_secure_connection_available();
BT_COMMON_API bool ble_is_peripheral_role_supported();

// Instance-based caller
typedef void* BluetoothCallerPtr;
typedef void (*BluetoothStatusCallback)(bool is_on);
typedef void (*PairingCallback)(bool success, void* user_data);

// callbacks to register and unregister a listener
BT_COMMON_API BluetoothCallerPtr bluetooth_caller_register_listener(BluetoothStatusCallback callback);
BT_COMMON_API void bluetooth_caller_unregister_listener(BluetoothCallerPtr caller);

#ifdef __cplusplus
}
#endif

#endif // BT_COMMON_C_API_H
