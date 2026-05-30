#include "ble_advertiser.h"
#include <map>
#include <mutex>
#include <string>
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

ble_advertiser::ble_advertiser() { init_apartment(); }

ble_advertiser::~ble_advertiser() {
    if (m_service_provider) {
        if (const auto status = m_service_provider.AdvertisementStatus();
            status == GattServiceProviderAdvertisementStatus::Started ||
            status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
            m_service_provider.StopAdvertising();
        }
    }
}

void ble_advertiser::register_callbacks(const BLEAdvertiserCallbacks& callbacks) {
    std::lock_guard lock(m_mutex);
    m_callbacks = callbacks;
}

int32_t ble_advertiser::get_status() const {
    if (!m_service_provider) return -1;
    return static_cast<int32_t>(m_service_provider.AdvertisementStatus());
}

void ble_advertiser::start(const BLEAdvertiseConfig& config) const {
    if (!m_service_provider) return;

    const GattServiceProviderAdvertisingParameters params;
    params.IsConnectable(config.connectable);
    params.IsDiscoverable(config.discoverable);

    if (config.service_data && config.service_data_len > 0) {
        const DataWriter writer;
        writer.WriteBytes(array_view(config.service_data, config.service_data + config.service_data_len));
        params.ServiceData(writer.DetachBuffer());
    }

    m_service_provider.StartAdvertising(params);
}

void ble_advertiser::stop() const {
    if (!m_service_provider) return;
    m_service_provider.StopAdvertising();
}

void ble_advertiser::add_service(const char* service_uuid) {
    const std::wstring w_uuid = to_hstring(service_uuid).c_str();
    const auto result         = GattServiceProvider::CreateAsync(guid(w_uuid)).get();

    if (m_callbacks.on_service_added) {
        m_callbacks.on_service_added(service_uuid, static_cast<int32_t>(result.Error()), m_callbacks.user_data);
    }

    if (result.Error() == BluetoothError::Success) {
        m_service_provider = result.ServiceProvider();

        m_service_provider.AdvertisementStatusChanged(
            [this](GattServiceProvider const&, GattServiceProviderAdvertisementStatusChangedEventArgs const& args) {
                if (m_callbacks.on_service_status_change) {
                    m_callbacks.on_service_status_change(static_cast<int32_t>(args.Status()), m_callbacks.user_data);
                }
            } );
    }
}

void ble_advertiser::add_characteristic(const ble_characteristics characteristics) {
    if (!m_service_provider) return;

    std::wstring w_uuid = to_hstring(characteristics.characteristic_uuid).c_str();
    const GattLocalCharacteristicParameters params;
    auto properties = GattCharacteristicProperties::None;

    if (characteristics.can_read) {
        properties |= GattCharacteristicProperties::Read;
        params.ReadProtectionLevel(GattProtectionLevel::Plain);
    }
    if (characteristics.can_write) {
        properties |= GattCharacteristicProperties::Write;
        params.WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
    }
    if (characteristics.can_write_no_response) {
        properties |= GattCharacteristicProperties::WriteWithoutResponse;
        params.WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
    }
    if (characteristics.can_notify) properties |= GattCharacteristicProperties::Notify;
    if (characteristics.can_indicate) properties |= GattCharacteristicProperties::Indicate;

    params.CharacteristicProperties(properties);

    const auto result         = m_service_provider.Service().CreateCharacteristicAsync(guid(w_uuid), params).get();
    const auto characteristic = result.Characteristic();
    m_characteristics.insert(std::make_pair(w_uuid, characteristic));

    auto w_service_uuid = to_hstring(m_service_provider.Service().Uuid());

    characteristic.ReadRequested(
        [this, w_service_uuid, w_uuid](GattLocalCharacteristic const&, GattReadRequestedEventArgs const& args) {
            const auto deferral  = args.GetDeferral();
            const auto request   = args.GetRequestAsync().get();
            const auto device_id = args.Session().DeviceId().Id();

            if (m_callbacks.on_read_characteristic) {
                auto* req_ctx = new BLERequestContext(args, request, deferral);
                m_callbacks.on_read_characteristic(reinterpret_cast<BLERequestHandle>(req_ctx),
                                                   to_string(device_id).c_str(), to_string(w_service_uuid).c_str(),
                                                   to_string(w_uuid).c_str(), 0, m_callbacks.user_data);
            } else {
                request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                deferral.Complete();
            }
        });

    characteristic.WriteRequested(
        [this, w_service_uuid, w_uuid](GattLocalCharacteristic const&, GattWriteRequestedEventArgs const& args) {
            const auto deferral  = args.GetDeferral();
            const auto request   = args.GetRequestAsync().get();
            const auto device_id = args.Session().DeviceId().Id();
            const auto buffer    = request.Value();

            if (m_callbacks.on_write_characteristic) {
                auto* req_ctx = new BLERequestContext(args, request, deferral);
                m_callbacks.on_write_characteristic(
                    reinterpret_cast<BLERequestHandle>(req_ctx), to_string(device_id).c_str(),
                    to_string(w_service_uuid).c_str(), to_string(w_uuid).c_str(), buffer.data(), buffer.Length(),
                    request.Option() == GattWriteOption::WriteWithResponse, m_callbacks.user_data);

                // deferral should be handled from read_response_call
            } else {
                deferral.Complete();
            }
        });
}

void ble_advertiser::add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid) {
    std::wstring w_char_uuid = to_hstring(characteristic_uuid).c_str();
    const auto it            = m_characteristics.find(w_char_uuid);
    if (it == m_characteristics.end()) return;

    const auto characteristic = it->second;
    std::wstring w_desc_uuid  = to_hstring(descriptor_uuid).c_str();
    auto w_service_uuid       = to_hstring(m_service_provider.Service().Uuid());

    const GattLocalDescriptorParameters params;
    params.ReadProtectionLevel(GattProtectionLevel::Plain);
    params.WriteProtectionLevel(GattProtectionLevel::Plain);

    const auto result     = characteristic.CreateDescriptorAsync(guid(w_desc_uuid), params).get();
    const auto descriptor = result.Descriptor();

    descriptor.ReadRequested([this, w_service_uuid, w_char_uuid, w_desc_uuid](GattLocalDescriptor const&,
                                                                              GattReadRequestedEventArgs const& args) {
        const auto deferral  = args.GetDeferral();
        const auto request   = args.GetRequestAsync().get();
        const auto device_id = args.Session().DeviceId().Id();

        if (m_callbacks.on_read_descriptor) {
            auto* req_ctx = new BLERequestContext(args, request, deferral);
            m_callbacks.on_read_descriptor(reinterpret_cast<BLERequestHandle>(req_ctx), to_string(device_id).c_str(),
                                           to_string(w_service_uuid).c_str(), to_string(w_char_uuid).c_str(),
                                           to_string(w_desc_uuid).c_str(), 0, m_callbacks.user_data);
        } else {
            request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
            deferral.Complete();
        }
    });

    descriptor.WriteRequested([this, w_service_uuid, w_char_uuid,
                               w_desc_uuid](GattLocalDescriptor const&, GattWriteRequestedEventArgs const& args) {
        const auto deferral  = args.GetDeferral();
        const auto request   = args.GetRequestAsync().get();
        const auto device_id = args.Session().DeviceId().Id();
        const auto buffer    = request.Value();

        if (m_callbacks.on_write_descriptor) {
            auto* req_ctx = new BLERequestContext(args, request, deferral);
            m_callbacks.on_write_descriptor(reinterpret_cast<BLERequestHandle>(req_ctx), to_string(device_id).c_str(),
                                            to_string(w_service_uuid).c_str(), to_string(w_char_uuid).c_str(),
                                            to_string(w_desc_uuid).c_str(), buffer.data(), buffer.Length(),
                                            true, // Descriptors usually write with response
                                            m_callbacks.user_data);
        } else {
            deferral.Complete();
        }
    });
}

void ble_advertiser::send_notification(const char* device_address, const char* characteristic_uuid,
                                       const uint8_t* value, const size_t value_len) {
    std::wstring w_char_uuid = to_hstring(characteristic_uuid).c_str();
    const auto it            = m_characteristics.find(w_char_uuid);
    if (it == m_characteristics.end()) return;

    const auto characteristic  = it->second;
    std::wstring w_device_addr = to_hstring(device_address).c_str();

    const DataWriter writer;
    writer.WriteBytes(array_view(value, value + value_len));
    const auto buffer = writer.DetachBuffer();

    const auto subscribed_clients      = characteristic.SubscribedClients();
    GattSubscribedClient target_client = nullptr;

    for (auto&& client : subscribed_clients) {
        if (client.Session().DeviceId().Id() == w_device_addr) {
            target_client = client;
            break;
        }
    }

    if (target_client) {
        const auto operation = characteristic.NotifyValueAsync(buffer, target_client);

        const bool is_indication = (characteristic.CharacteristicProperties() &
                                    GattCharacteristicProperties::Indicate) == GattCharacteristicProperties::Indicate;
        if (is_indication) {
            operation.Completed([this, w_device_addr, w_char_uuid](auto const& async_op, AsyncStatus const status) {
                if (status == AsyncStatus::Completed) {
                    auto result = async_op.GetResults();
                    if (m_callbacks.on_indication_result) {
                        m_callbacks.on_indication_result(
                            to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(), true,
                            static_cast<int32_t>(result.Status()), 0, m_callbacks.user_data);
                    }
                } else if (status == AsyncStatus::Error) {
                    if (m_callbacks.on_indication_result) {
                        m_callbacks.on_indication_result(to_string(w_device_addr).c_str(),
                                                         to_string(w_char_uuid).c_str(), false, -1,
                                                         async_op.ErrorCode().value, m_callbacks.user_data);
                    }
                }
            });
        }
    }
}

void ble_advertiser::respond_read(BLERequestHandle request, const uint8_t* data, const size_t len,
                                  const int32_t status) {
    auto* req_ctx = static_cast<BLERequestContext*>(request);
    if (!req_ctx) return;

    if (auto* read = std::get_if<BLERequestContext::Read>(&req_ctx->data)) {
        if (status == 0) {
            // Success
            const DataWriter writer;
            writer.WriteBytes(array_view(data, data + len));
            read->request.RespondWithValue(writer.DetachBuffer());
        } else {
            read->request.RespondWithProtocolError(static_cast<uint8_t>(status));
        }
        read->deferral.Complete();
    }

    delete req_ctx;
}

void ble_advertiser::respond_write(BLERequestHandle request, const int32_t status) {
    auto* req_ctx = static_cast<BLERequestContext*>(request);
    if (!req_ctx) return;

    if (auto* write = std::get_if<BLERequestContext::Write>(&req_ctx->data)) {
        if (write->request.Option() == GattWriteOption::WriteWithResponse) {
            if (status == 0)
                write->request.Respond();
            else {
                write->request.RespondWithProtocolError(static_cast<uint8_t>(status));
            }
        }
        write->deferral.Complete();
    }

    delete req_ctx;
}
