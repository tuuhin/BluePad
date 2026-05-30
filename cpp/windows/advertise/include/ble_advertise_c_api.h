#ifndef BLE_ADVERTISE_C_API_H
#define BLE_ADVERTISE_C_API_H

#include "ble_models.h"
#include <stdbool.h>
#include <stdint.h>

#ifdef _MSC_VER
#ifdef BT_ADVERTISE_EXPORTS
#define BT_ADVERTISE_API __declspec(dllexport)
#else
#define BT_ADVERTISE_API __declspec(dllimport)
#endif
#else
#define BT_ADVERTISE_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef void* BLEAdvertiserPtr;
typedef void* BLERequestHandle;

typedef struct {
    bool discoverable;
    bool connectable;
    const uint8_t* service_data;
    size_t service_data_len;
} BLEAdvertiseConfig;

// Callbacks
typedef void (*OnServiceAddedCallback)(
    // service uuid
    const char* service_uuid,
    // error code
    int32_t error_code,
    // data pointer
    void* user_data);

typedef void (*OnServiceStatusChangeCallback)(
    // status
    int32_t status,
    // data pointer
    void* user_data);

typedef void (*OnReadCharacteristicCallback)(
    // request handle
    BLERequestHandle request,
    // device address
    const char* device_address,
    // service uuid
    const char* service_uuid,
    // characteristics uuid
    const char* characteristic_uuid,
    // read status
    int32_t status,
    // data pointer
    void* user_data);

typedef void (*OnWriteCharacteristicCallback)(
    // request handle
    BLERequestHandle request,
    // device address
    const char* device_address,
    // service uuid
    const char* service_uuid,
    // characteristics
    const char* characteristic_uuid,
    // incoming bytearray as data pointer and length
    const uint8_t* data, size_t len,
    // marker if response is needed
    bool respond_needed,
    // data pointer
    void* user_data);

typedef void (*OnReadDescriptorCallback)(
    // request handle
    BLERequestHandle request,
    // device address
    const char* device_address,
    // service uuid
    const char* service_uuid,
    // characteristics uuid
    const char* characteristic_uuid,
    // descriptor uuid
    const char* descriptor_uuid,
    // read status
    int32_t status,
    // data pointer
    void* user_data);

typedef void (*OnWriteDescriptorCallback)(
    // request handle
    BLERequestHandle request,
    // device address
    const char* device_address,
    // service uuid
    const char* service_uuid,
    // characteristics uuid
    const char* characteristic_uuid,
    // descriptor uuid
    const char* descriptor_uuid,
    // incoming bytearray as data pointer and length
    const uint8_t* data, size_t len,
    // is response needed
    bool respond_needed,
    // data pointer
    void* user_data);

typedef void (*OnIndicationResultCallback)(
    // device address
    const char* device_address,
    // characteristics uuid
    const char* characteristic_uuid,
    // is indication success
    bool success,
    // status
    int32_t status,
    // error code
    int32_t error_code,
    // data pointer
    void* user_data);

typedef struct {
    void* user_data;
    OnServiceAddedCallback on_service_added;
    OnServiceStatusChangeCallback on_service_status_change;
    OnReadCharacteristicCallback on_read_characteristic;
    OnWriteCharacteristicCallback on_write_characteristic;
    OnReadDescriptorCallback on_read_descriptor;
    OnWriteDescriptorCallback on_write_descriptor;
    OnIndicationResultCallback on_indication_result;
} BLEAdvertiserCallbacks;

BT_ADVERTISE_API BLEAdvertiserPtr ble_advertiser_create();
BT_ADVERTISE_API void ble_advertiser_destroy(BLEAdvertiserPtr advertiser);

BT_ADVERTISE_API void ble_advertiser_register_callbacks(BLEAdvertiserPtr advertiser, BLEAdvertiserCallbacks callbacks);
BT_ADVERTISE_API int32_t ble_advertiser_get_status(BLEAdvertiserPtr advertiser);
BT_ADVERTISE_API void ble_advertiser_start(BLEAdvertiserPtr advertiser, BLEAdvertiseConfig config);
BT_ADVERTISE_API void ble_advertiser_stop(BLEAdvertiserPtr advertiser);

BT_ADVERTISE_API void ble_advertiser_add_service(BLEAdvertiserPtr advertiser, const char* service_uuid);
inline BT_ADVERTISE_API void ble_advertiser_add_characteristic(BLEAdvertiserPtr advertiser,
                                                               ble_characteristics characteristics) ;
BT_ADVERTISE_API void ble_advertiser_add_descriptor(BLEAdvertiserPtr advertiser, const char* characteristic_uuid,
                                                    const char* descriptor_uuid);

BT_ADVERTISE_API void ble_advertiser_send_notification(BLEAdvertiserPtr advertiser, const char* device_address,
                                                       const char* characteristic_uuid, const uint8_t* value,
                                                       size_t value_len);

// Response functions
BT_ADVERTISE_API void ble_advertiser_respond_read(BLERequestHandle request, const uint8_t* data, size_t len,
                                                  int32_t status);
BT_ADVERTISE_API void ble_advertiser_respond_write(BLERequestHandle request, int32_t status);

#ifdef __cplusplus
}
#endif

#endif // BLE_ADVERTISE_C_API_H
