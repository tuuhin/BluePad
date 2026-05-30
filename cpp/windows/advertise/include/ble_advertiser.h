#ifndef BLE_ADVERTISER_H
#define BLE_ADVERTISER_H

#include "ble_advertise_c_api.h"
#include <map>
#include <mutex>
#include <string>
#include <variant>
#include <vector>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Storage.Streams.h>

using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Foundation;

struct BLERequestContext {
    struct Read {
        GattReadRequestedEventArgs args;
        GattReadRequest request;
        Deferral deferral;
    };

    struct Write {
        GattWriteRequestedEventArgs args;
        GattWriteRequest request;
        Deferral deferral;
    };

    std::variant<Read, Write> data;

    BLERequestContext(GattReadRequestedEventArgs const& a, GattReadRequest const& r, Deferral const& d)
        : data(Read{a, r, d}) {}

    BLERequestContext(GattWriteRequestedEventArgs const& a, GattWriteRequest const& r, Deferral const& d)
        : data(Write{a, r, d}) {}
};

class ble_advertiser {
public:
    ble_advertiser();
    ~ble_advertiser();

    void register_callbacks(const BLEAdvertiserCallbacks& callbacks);
    [[nodiscard]] int32_t get_status() const;
    void start(const BLEAdvertiseConfig& config) const;
    void stop() const;

    void add_service(const char* service_uuid);
    void add_characteristic(ble_characteristics characteristics);
    void add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid);

    void send_notification(const char* device_address, const char* characteristic_uuid, const uint8_t* value,
                           size_t value_len);

    static void respond_read(BLERequestHandle request, const uint8_t* data, size_t len, int32_t status);
    static void respond_write(BLERequestHandle request, int32_t status);

private:
    GattServiceProvider m_service_provider = nullptr;
    std::map<std::wstring, GattLocalCharacteristic> m_characteristics;
    BLEAdvertiserCallbacks m_callbacks = {nullptr};
    std::mutex m_mutex;
};

#endif // BLE_ADVERTISER_H
