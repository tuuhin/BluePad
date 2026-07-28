#ifndef WINDOWS_BLE_BLUETOOTH_ENUMS_H
#define WINDOWS_BLE_BLUETOOTH_ENUMS_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum bt_bond_state : int32_t {
    DEVICE_BONDED        = 1,
    DEVICE_NOT_BONDED    = 2,
    ERROR_INVALID_DEVICE = 3,
    ERROR_UNKNOWN        = 4,
};

enum bt_bond_response : int32_t {
    RESPONSE_PAIRED                      = 0,
    RESPONSE_NOT_READY_TO_PAIR           = 1,
    RESPONSE_NOT_PAIRED                  = 2,
    RESPONSE_ALREADY_PAIRED              = 3,
    RESPONSE_CONNECTION_REJECTED         = 4,
    RESPONSE_TOO_MANY_CONNECTION         = 5,
    RESPONSE_HARDWARE_FAILURE            = 6,
    RESPONSE_AUTHENTICATION_TIMEOUT      = 7,
    RESPONSE_AUTHENTICATION_NOT_ALLOWED  = 8,
    RESPONSE_AUTHENTICATION_FAILURE      = 9,
    RESPONSE_NO_SUPPORTED_PROFILES       = 10,
    RESPONSE_PROTECTION_LEVEL_ISSUES     = 11,
    RESPONSE_ACCESS_DENIED               = 12,
    RESPONSE_INVALID_DATA                = 13,
    RESPONSE_PARING_OPERATION_CANCELLED  = 14,
    RESPONSE_OPERATION_IN_PROGRESS       = 15,
    RESPONSE_HANDLER_NOT_REGISTERED      = 16,
    RESPONSE_REJECTED_BY_HANDLER         = 17,
    RESPONSE_REMOVE_DEVICE_IS_ASSOCIATED = 18,
    RESPONSE_FAILED                      = 19,

    RESPONSE_ERROR_UNKNOWN             = 100,
    RESPONSE_ERROR_OPERATION_CANCELLED = 101,
};

enum bt_bond_request_error_code : int32_t {
    ERROR_DEVICE_ALREADY_BONDED   = 1,
    ERROR_DEVICE_CANNOT_BE_BONDED = 2,
};

enum bt_request_enable_status : int32_t {
    REQUEST_ACCEPTED                   = 0,
    REQUEST_DENIED_PRIVACY_ISSUES      = 1,
    REQUEST_DENIED_BY_SYSTEM           = 2,
    REQUEST_DENIED_BY_USER             = 3,
    REQUEST_DENIED_CANNOT_FIND_ADAPTER = 4,
    REQUEST_DENIED_UNKNOWN             = 5,
    REQUEST_NOT_NEEDED                 = 6,
};

#ifdef __cplusplus
}
#endif

#endif // WINDOWS_BLE_BLUETOOTH_ENUMS_H
