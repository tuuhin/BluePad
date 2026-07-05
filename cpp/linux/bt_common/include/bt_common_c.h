#ifndef LINUX_BT_COMMON_C_H
#define LINUX_BT_COMMON_C_H

#include <stdbool.h>
#include <stdint.h>

#include "bt_common_defination.h"

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
BT_COMMON_API void init_logger();
BT_COMMON_API bool ble_is_bluetooth_active();
BT_COMMON_API bool ble_is_secure_connection_available();
BT_COMMON_API bool ble_is_peripheral_role_supported();

// functions to check the pairing capabilities
BT_COMMON_API enum bt_bond_state is_device_bonded(const char* device_address);
BT_COMMON_API enum bt_bond_response request_bond(const char* device_address, uint32_t timeout_in_millis);

// callbacks to register and unregister a listener
BT_COMMON_API void bluetooth_caller_register_listener(BluetoothStatusCallback callback);
BT_COMMON_API void bluetooth_caller_unregister_listener();

BT_COMMON_API bt_bond_manager_handle create_bond_manager();
BT_COMMON_API void destroy_bond_manager(bt_bond_manager_handle handle);
BT_COMMON_API void bond_manager_request_pairing(bt_bond_manager_handle handle, const char* device_address,
                                                bt_bond_pairing_callback callback);
BT_COMMON_API void bond_manager_unregister_pairing(bt_bond_manager_handle handle);
BT_COMMON_API void bond_manager_accept_connection(bt_bond_manager_handle handle, const char* pin,
                                                  bt_bond_responder_handle responder);
BT_COMMON_API void bond_manager_reject_connection(bt_bond_manager_handle handle, bt_bond_responder_handle responder);
#ifdef __cplusplus
}
#endif

#endif // LINUX_BT_COMMON_C_H
