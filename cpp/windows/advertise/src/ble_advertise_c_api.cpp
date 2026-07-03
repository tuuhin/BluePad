#include <iostream>
#include <memory>

#include "ble_advertise_c_api.h"
#include "ble_advertiser.h"
#include "utils.h"

extern "C" {
BLEAdvertiserPtr ble_advertiser_create() {
    utils::init_logger();
    return new std::shared_ptr(std::make_shared<ble_advertiser>());
}

void ble_advertiser_destroy(BLEAdvertiserPtr advertiser) {
    delete static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
}

void ble_advertiser_register_callbacks(BLEAdvertiserPtr advertiser, BLEAdvertiserCallbacks callbacks) {
    auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->register_callbacks(callbacks);
}

int32_t ble_advertiser_get_status(BLEAdvertiserPtr advertiser) {
    const auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return -1;
    const auto* ctx = advertiser_ptr->get();
    if (!ctx) return -1;
    return ctx->get_status();
}

void ble_advertiser_start(BLEAdvertiserPtr advertiser, BLEAdvertiseConfig config) {
    const auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    const auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->start(config);
}

void ble_advertiser_stop(BLEAdvertiserPtr advertiser) {
    const auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    const auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->stop();
}

void ble_advertiser_add_service(BLEAdvertiserPtr advertiser, const char* service_uuid) {
    auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->add_service(service_uuid);
}

void ble_advertiser_add_characteristic(BLEAdvertiserPtr advertiser, ble_characteristics characteristics) {
    auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->add_characteristic(characteristics);
}

void ble_advertiser_add_descriptor(BLEAdvertiserPtr advertiser, const char* characteristic_uuid,
                                   const char* descriptor_uuid) {
    auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return;
    auto* ctx = advertiser_ptr->get();
    if (!ctx) return;
    ctx->add_descriptor(characteristic_uuid, descriptor_uuid);
}

bool ble_advertiser_send_notification(BLEAdvertiserPtr advertiser, const char* device_address,
                                      const char* characteristic_uuid, const uint8_t* value, size_t value_len) {
    auto* advertiser_ptr = static_cast<std::shared_ptr<ble_advertiser>*>(advertiser);
    if (!advertiser_ptr || !*advertiser_ptr) return false;
    auto* ctx = advertiser_ptr->get();
    if (ctx == nullptr) return false;
    return ctx->send_notification(device_address, characteristic_uuid, value, value_len);
}

void ble_advertiser_respond_read(BLERequestHandle request, const uint8_t* data, size_t len, int32_t status) {
    auto* raw_ctx = static_cast<BLERequestContext*>(request);
    if (!raw_ctx) return;

    std::unique_ptr<BLERequestContext> reclaimed_ptr(raw_ctx);
    ble_advertiser::respond_read(std::move(reclaimed_ptr), data, len, status);
}

void ble_advertiser_respond_write(BLERequestHandle request, int32_t status) {
    auto* raw_ctx = static_cast<BLERequestContext*>(request);
    if (!raw_ctx) return;

    std::unique_ptr<BLERequestContext> reclaimed_ptr(raw_ctx);
    ble_advertiser::respond_write(std::move(reclaimed_ptr), status);
}
}
