#include "ble_advertise_c_api.h"
#include "ble_advertiser.h"
#include <iostream>

extern "C" {
BLEAdvertiserPtr ble_advertiser_create() { return new ble_advertiser(); }

void ble_advertiser_destroy(BLEAdvertiserPtr advertiser) {
    const auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    delete ctx;
}

void ble_advertiser_register_callbacks(BLEAdvertiserPtr advertiser, BLEAdvertiserCallbacks callbacks) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->register_callbacks(callbacks);
}

int32_t ble_advertiser_get_status(BLEAdvertiserPtr advertiser) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return -1;
    return ctx->get_status();
}

void ble_advertiser_start(BLEAdvertiserPtr advertiser, BLEAdvertiseConfig config) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->start(config);
}

void ble_advertiser_stop(BLEAdvertiserPtr advertiser) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->stop();
}

void ble_advertiser_add_service(BLEAdvertiserPtr advertiser, const char* service_uuid) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->add_service(service_uuid);
}

void ble_advertiser_add_characteristic(BLEAdvertiserPtr advertiser,ble_characteristics characteristics) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->add_characteristic(characteristics);
}

void ble_advertiser_add_descriptor(BLEAdvertiserPtr advertiser, const char* characteristic_uuid,
                                   const char* descriptor_uuid) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (!ctx) return;
    ctx->add_descriptor(characteristic_uuid, descriptor_uuid);
}

bool ble_advertiser_send_notification(BLEAdvertiserPtr advertiser, const char* device_address,
                                      const char* characteristic_uuid, const uint8_t* value, size_t value_len) {
    auto* ctx = static_cast<ble_advertiser*>(advertiser);
    if (ctx == nullptr) return false;
    return ctx->send_notification(device_address, characteristic_uuid, value, value_len);
}

void ble_advertiser_respond_read(BLERequestHandle request, const uint8_t* data, size_t len, int32_t status) {
    ble_advertiser::respond_read(request, data, len, status);
}

void ble_advertiser_respond_write(BLERequestHandle request, int32_t status) {
    ble_advertiser::respond_write(request, status);
}
}
