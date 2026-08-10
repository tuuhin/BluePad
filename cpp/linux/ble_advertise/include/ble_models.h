#ifndef BLE_MODELS_H
#define BLE_MODELS_H

#include <stdbool.h>

typedef struct {
    const char* characteristic_uuid;
    bool can_read;
    bool can_write;
    bool can_write_no_response;
    bool can_notify;
    bool can_indicate;
} ble_characteristics;

#endif
