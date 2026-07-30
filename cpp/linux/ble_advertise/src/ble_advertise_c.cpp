#include <memory>

#include "ble_advertise_c.h"
#include "ble_advertiser.h"
#include "bt_utils.h"

using AdvertiserSharedPtr = std::shared_ptr<ble_advertiser>;
static ble_advertiser* get_instance(BLEAdvertiserPtr handle) noexcept {
    if (!handle) return nullptr;
    const auto* shared_handle = static_cast<AdvertiserSharedPtr*>(handle);
    return (shared_handle && *shared_handle) ? shared_handle->get() : nullptr;
}

extern "C" {
BLEAdvertiserPtr ble_advertiser_create() {
    utils::init_logger();
    auto instance = std::make_shared<ble_advertiser>();
    return new AdvertiserSharedPtr(std::move(instance));
}

void ble_advertiser_destroy(BLEAdvertiserPtr advertiser) {
    if (advertiser == nullptr) return;
    delete static_cast<AdvertiserSharedPtr*>(advertiser);
}

void ble_advertiser_register_callbacks(BLEAdvertiserPtr advertiser, BLEAdvertiserCallbacks callbacks) {
    auto* ctx = get_instance(advertiser);
    ctx->register_callbacks(callbacks);
}

int32_t ble_advertiser_get_status(BLEAdvertiserPtr advertiser) {
    const auto* ctx = get_instance(advertiser);
    return ctx->get_status();
}

void ble_advertiser_start(BLEAdvertiserPtr advertiser, BLEAdvertiseConfig config) {
    auto* ctx = get_instance(advertiser);
    ctx->start(config);
}

void ble_advertiser_stop(BLEAdvertiserPtr advertiser) {
    auto* ctx = get_instance(advertiser);
    ctx->stop();
}

void ble_advertiser_add_service(BLEAdvertiserPtr advertiser, const char* service_uuid) {
    auto* ctx = get_instance(advertiser);
    ctx->add_service(service_uuid);
}

void ble_advertiser_add_characteristic(BLEAdvertiserPtr advertiser, ble_characteristics characteristics) {
    const auto* ctx = get_instance(advertiser);
    ctx->add_characteristic(characteristics);
}

void ble_advertiser_add_descriptor(BLEAdvertiserPtr advertiser, const char* characteristic_uuid,
                                   const char* descriptor_uuid) {
    const auto* ctx = get_instance(advertiser);
    ctx->add_descriptor(characteristic_uuid, descriptor_uuid);
}

bool ble_advertiser_send_notification(BLEAdvertiserPtr advertiser, const char* device_address,
                                      const char* characteristic_uuid, const uint8_t* value, size_t value_len) {
    const auto* ctx = get_instance(advertiser);
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