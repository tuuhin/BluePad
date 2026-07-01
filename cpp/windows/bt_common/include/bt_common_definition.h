#ifndef BT_COMMON_DEFINITION
#define BT_COMMON_DEFINITION

#include "bluetooth_enums.h"

// Instance-based caller for bluetooth caller
typedef void* BluetoothCallerPtr;
typedef void (*BluetoothStatusCallback)(bool is_on);

// instance handle for the bond_manager class
typedef void* bt_bond_manager_handle;

// instance handle to store the args references for device pairing args
typedef void* bt_bond_responder_handle;

typedef void (*bt_on_bond_error)(enum bt_bond_request_error_code code);
typedef void (*bt_on_bond_results)(enum bt_bond_response resp);
typedef void (*bt_bond_on_req_confirm_pin)(const char* display_pin, void* data);

typedef struct {
    bt_on_bond_results on_results;
    bt_bond_on_req_confirm_pin on_confirm_pin;
    bt_on_bond_error on_error;
} bt_bond_pairing_callback;

#endif
