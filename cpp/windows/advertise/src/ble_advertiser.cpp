#include "ble_advertiser.h"
#include <iostream>
#include <map>
#include <mutex>
#include <string>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Storage.Streams.h>

#define WIN_LOG(msg) std::wclog << "[NATIVE-WINDOWS] " << msg << std::endl;

using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Foundation;

namespace {
hstring to_clean_hstring(guid const& g) {
    return hstring{std::format(L"{:08x}-{:04x}-{:04x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}", g.Data1,
                               g.Data2, g.Data3, g.Data4[0], g.Data4[1], g.Data4[2], g.Data4[3], g.Data4[4], g.Data4[5],
                               g.Data4[6], g.Data4[7])};
}

const char* log_advertisement_status(const GattServiceProviderAdvertisementStatus status) {
    switch (status) {
    case GattServiceProviderAdvertisementStatus::Created:
        return "Created";
    case GattServiceProviderAdvertisementStatus::Aborted:
        return "Aborted";
    case GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData:
        return "Started without advertisement data";
    case GattServiceProviderAdvertisementStatus::Started:
        return "Started";
    case GattServiceProviderAdvertisementStatus::Stopped:
        return "Stopped";
    default:
        return "Unknown";
    }
}

} // namespace

ble_advertiser::ble_advertiser() {
    init_apartment();
    WIN_LOG(L"BLE ADVERTISER INITIALIZED");
}

ble_advertiser::~ble_advertiser() {
    WIN_LOG(L"BLE ADVERTISER DESTRUCTOR CALLED");
    std::lock_guard lock(m_mutex);
    if (m_service_provider) {
        try {
            const auto status = m_service_provider.AdvertisementStatus();
            if (status == GattServiceProviderAdvertisementStatus::Started ||
                status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
                WIN_LOG(L"BLE ADVERTISER STATUS WAS " << log_advertisement_status(status));
                m_service_provider.StopAdvertising();
            }
        } catch (hresult_error const& ex) {
            WIN_LOG(L"WINRT EXCEPTION IN DESTRUCTOR: " << ex.message().c_str());
        }
        m_service_provider = nullptr;
    }
}

void ble_advertiser::register_callbacks(const BLEAdvertiserCallbacks& callbacks) {
    std::lock_guard lock(m_mutex);
    m_callbacks = callbacks;
    WIN_LOG(L"BLE ADVERTISEMENT CALLBACK CREATED");
}

int32_t ble_advertiser::get_status() const {
    std::lock_guard lock(m_mutex);
    if (!m_service_provider) {
        return -1;
    }
    return static_cast<int32_t>(m_service_provider.AdvertisementStatus());
}

void ble_advertiser::start(const BLEAdvertiseConfig& config) const {
    std::lock_guard lock(m_mutex);
    if (!m_service_provider) {
        WIN_LOG(L"SERVICE PROVIDER NOT INITIALIZED");
        return;
    }

    try {
        const GattServiceProviderAdvertisingParameters params;
        params.IsConnectable(config.connectable);
        params.IsDiscoverable(config.discoverable);

        if (config.service_data && config.service_data_len > 0) {
            const DataWriter writer;
            writer.WriteBytes(array_view(config.service_data, config.service_data + config.service_data_len));
            params.ServiceData(writer.DetachBuffer());
        }
        m_service_provider.StartAdvertising(params);
        WIN_LOG(L"ADVERTISEMENT STARTED FOR THE GIVEN CONFIG");

        const auto status = m_service_provider.AdvertisementStatus();
        WIN_LOG(L"ADVERTISING STATUS AFTER START ADVERTISING " << log_advertisement_status(status));

    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN START: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN START");
    }
}

void ble_advertiser::stop() const {
    std::lock_guard lock(m_mutex);
    if (!m_service_provider) return;

    try {
        const auto status = m_service_provider.AdvertisementStatus();
        WIN_LOG(L"ADVERTISING STATUS  BEFORE STOP REQUESTED " << log_advertisement_status(status));

        if (status == GattServiceProviderAdvertisementStatus::Started ||
            status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
            m_service_provider.StopAdvertising();

            // read the status again after stop
            const auto status = m_service_provider.AdvertisementStatus();
            WIN_LOG(L"ADVERTISING STATUS AFTER STOP ADVERTISING " << log_advertisement_status(status));
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN STOP: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN START");
    }
}

void ble_advertiser::add_service(const char* service_uuid) {
    std::lock_guard lock(m_mutex);
    try {
        const hstring h_uuid = to_hstring(service_uuid);
        WIN_LOG(L"ADDING SERVICE: " << h_uuid.c_str());

        const guid service_guid{h_uuid};
        const auto result = GattServiceProvider::CreateAsync(service_guid).get();

        if (m_callbacks.on_service_added) {
            m_callbacks.on_service_added(service_uuid, static_cast<int32_t>(result.Error()), m_callbacks.user_data);
        }

        if (result.Error() == BluetoothError::Success) {
            WIN_LOG(L"SERVICE ADDED SUCCESSFULLY SERVICE ID: " << h_uuid.c_str());
            m_service_provider = result.ServiceProvider();

            m_service_provider.AdvertisementStatusChanged(
                [this](GattServiceProvider const&, GattServiceProviderAdvertisementStatusChangedEventArgs const& args) {
                    WIN_LOG(L"ADVERTISEMENT STATUS CHANGED : " << log_advertisement_status(args.Status()));
                    std::lock_guard cb_lock(m_mutex);
                    if (m_callbacks.on_service_status_change) {
                        m_callbacks.on_service_status_change(static_cast<int32_t>(args.Status()),
                                                             m_callbacks.user_data);
                    }
                });
        } else {
            WIN_LOG(L"FAILED TO ADD SERVICE. ERROR: " << static_cast<int32_t>(result.Error()));
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN ADD_SERVICE: " << ex.message().c_str());
    } catch (std::exception const& std_ex) {
        WIN_LOG(L"STANDARD C++ EXCEPTION IN ADD_SERVICE" << std_ex.what());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN ADD_SERVICE");
    }
}

void ble_advertiser::add_characteristic(const ble_characteristics characteristics) {
    std::lock_guard lock(m_mutex);
    try {
        WIN_LOG(L"ADDING CHARACTERISTIC: " << to_hstring(characteristics.characteristic_uuid).c_str());
        if (!m_service_provider) {
            WIN_LOG(L"SERVICE PROVIDER NOT INITIALIZED, CANNOT ADD CHARACTERISTIC");
            return;
        }

        hstring h_uuid = to_hstring(characteristics.characteristic_uuid);
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

        const auto result         = m_service_provider.Service().CreateCharacteristicAsync(guid(h_uuid), params).get();
        const auto characteristic = result.Characteristic();
        m_characteristics.insert(std::make_pair(h_uuid, characteristic));

        hstring w_service_uuid = to_clean_hstring(m_service_provider.Service().Uuid());

        characteristic.ReadRequested(
            [this, w_service_uuid, h_uuid](GattLocalCharacteristic const&, GattReadRequestedEventArgs const& args) {
                WIN_LOG(L"CHARACTERISTIC READ REQUESTED. UUID: " << h_uuid.c_str());
                const auto deferral  = args.GetDeferral();
                const auto request   = args.GetRequestAsync().get();
                const auto device_id = args.Session().DeviceId().Id();

                std::lock_guard cb_lock(m_mutex);
                if (m_callbacks.on_read_characteristic) {
                    auto* req_ctx = new BLERequestContext(args, request, deferral);
                    m_callbacks.on_read_characteristic(reinterpret_cast<BLERequestHandle>(req_ctx),
                                                       to_string(device_id).c_str(), to_string(w_service_uuid).c_str(),
                                                       to_string(h_uuid).c_str(), 0, m_callbacks.user_data);
                } else {
                    WIN_LOG(L"NO READ CALLBACK REGISTERED FOR CHARACTERISTIC");
                    request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                    deferral.Complete();
                }
            });

        characteristic.WriteRequested(
            [this, w_service_uuid, h_uuid](GattLocalCharacteristic const&, GattWriteRequestedEventArgs const& args) {
                WIN_LOG(L"CHARACTERISTIC WRITE REQUESTED. UUID: " << h_uuid.c_str());
                const auto deferral  = args.GetDeferral();
                const auto request   = args.GetRequestAsync().get();
                const auto device_id = args.Session().DeviceId().Id();
                const auto buffer    = request.Value();

                std::lock_guard cb_lock(m_mutex);
                if (m_callbacks.on_write_characteristic) {
                    auto* req_ctx = new BLERequestContext(args, request, deferral);
                    m_callbacks.on_write_characteristic(
                        reinterpret_cast<BLERequestHandle>(req_ctx), to_string(device_id).c_str(),
                        to_string(w_service_uuid).c_str(), to_string(h_uuid).c_str(), buffer.data(), buffer.Length(),
                        request.Option() == GattWriteOption::WriteWithResponse, m_callbacks.user_data);

                    // deferral should be handled from read_response_call
                } else {
                    WIN_LOG(L"NO WRITE CALLBACK REGISTERED FOR CHARACTERISTIC");
                    deferral.Complete();
                }
            });
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN ADD_CHARACTERISTIC: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN ADD_CHARACTERISTIC");
    }
}

void ble_advertiser::add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid) {
    std::lock_guard lock(m_mutex);
    try {
        WIN_LOG(L"ADDING DESCRIPTOR: " << to_hstring(descriptor_uuid).c_str() << L" TO CHARACTERISTIC: "
                                       << to_hstring(characteristic_uuid).c_str());
        std::wstring w_char_uuid = to_hstring(characteristic_uuid).c_str();
        const auto it            = m_characteristics.find(w_char_uuid);
        if (it == m_characteristics.end()) {
            WIN_LOG(L"CHARACTERISTIC NOT FOUND, CANNOT ADD DESCRIPTOR");
            return;
        }

        const auto characteristic = it->second;
        std::wstring w_desc_uuid  = to_hstring(descriptor_uuid).c_str();
        auto w_service_uuid       = to_clean_hstring(m_service_provider.Service().Uuid());

        const GattLocalDescriptorParameters params;
        params.ReadProtectionLevel(GattProtectionLevel::Plain);
        params.WriteProtectionLevel(GattProtectionLevel::Plain);

        const auto result     = characteristic.CreateDescriptorAsync(guid(w_desc_uuid), params).get();
        const auto descriptor = result.Descriptor();

        descriptor.ReadRequested([this, w_service_uuid, w_char_uuid,
                                  w_desc_uuid](GattLocalDescriptor const&, GattReadRequestedEventArgs const& args) {
            WIN_LOG(L"DESCRIPTOR READ REQUESTED. UUID: " << w_desc_uuid.c_str());
            const auto deferral  = args.GetDeferral();
            const auto request   = args.GetRequestAsync().get();
            const auto device_id = args.Session().DeviceId().Id();

            std::lock_guard cb_lock(m_mutex);
            if (m_callbacks.on_read_descriptor) {
                auto* req_ctx = new BLERequestContext(args, request, deferral);
                m_callbacks.on_read_descriptor(reinterpret_cast<BLERequestHandle>(req_ctx),
                                               to_string(device_id).c_str(), to_string(w_service_uuid).c_str(),
                                               to_string(w_char_uuid).c_str(), to_string(w_desc_uuid).c_str(), 0,
                                               m_callbacks.user_data);
            } else {
                WIN_LOG(L"NO READ CALLBACK REGISTERED FOR DESCRIPTOR");
                request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                deferral.Complete();
            }
        });

        descriptor.WriteRequested([this, w_service_uuid, w_char_uuid,
                                   w_desc_uuid](GattLocalDescriptor const&, GattWriteRequestedEventArgs const& args) {
            WIN_LOG(L"DESCRIPTOR WRITE REQUESTED. UUID: " << w_desc_uuid.c_str());
            const auto deferral  = args.GetDeferral();
            const auto request   = args.GetRequestAsync().get();
            const auto device_id = args.Session().DeviceId().Id();
            const auto buffer    = request.Value();

            std::lock_guard cb_lock(m_mutex);
            if (m_callbacks.on_write_descriptor) {
                auto* req_ctx = new BLERequestContext(args, request, deferral);
                m_callbacks.on_write_descriptor(reinterpret_cast<BLERequestHandle>(req_ctx),
                                                to_string(device_id).c_str(), to_string(w_service_uuid).c_str(),
                                                to_string(w_char_uuid).c_str(), to_string(w_desc_uuid).c_str(),
                                                buffer.data(), buffer.Length(),
                                                true, // Descriptors usually write with response
                                                m_callbacks.user_data);
            } else {
                WIN_LOG(L"NO WRITE CALLBACK REGISTERED FOR DESCRIPTOR");
                deferral.Complete();
            }
        });
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN ADD_DESCRIPTOR: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN ADD_DESCRIPTOR");
    }
}

void ble_advertiser::send_notification(const char* device_address, const char* characteristic_uuid,
                                       const uint8_t* value, const size_t value_len) {
    std::lock_guard lock(m_mutex);
    WIN_LOG(L"SENDING NOTIFICATION TO: " << to_hstring(device_address).c_str() << L" CHAR: "
                                         << to_hstring(characteristic_uuid).c_str());

    std::wstring w_char_uuid = to_hstring(characteristic_uuid).c_str();
    const auto it            = m_characteristics.find(w_char_uuid);
    if (it == m_characteristics.end()) {
        WIN_LOG(L"CHARACTERISTIC NOT FOUND, CANNOT SEND NOTIFICATION");
        return;
    }

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

    if (target_client == nullptr) {
        WIN_LOG(L"TARGET CLIENT NOT FOUND FOR NOTIFICATION");
        return;
    }

    const auto operation = characteristic.NotifyValueAsync(buffer, target_client);

    const bool is_indication = (characteristic.CharacteristicProperties() & GattCharacteristicProperties::Indicate) ==
                               GattCharacteristicProperties::Indicate;

    if (!is_indication) {
        WIN_LOG(L"CHARACTERISTICS DON'T HAVE INDICATE PROPERTIES SO WE DON'T CARE ABOUT THE ACK");
        return;
    }

    try {
        operation.Completed([this, w_device_addr, w_char_uuid](auto const& async_op, AsyncStatus const status) {
            if (status == AsyncStatus::Completed) {
                auto result = async_op.GetResults();
                WIN_LOG(L"INDICATION SENT SUCCESSFULLY. STATUS: " << static_cast<int32_t>(result.Status()));
                std::lock_guard cb_lock(m_mutex);
                if (m_callbacks.on_indication_result) {
                    m_callbacks.on_indication_result(to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(),
                                                     true, static_cast<int32_t>(result.Status()), 0,
                                                     m_callbacks.user_data);
                }
            } else if (status == AsyncStatus::Error) {
                WIN_LOG(L"FAILED TO SEND INDICATION. ERROR: " << async_op.ErrorCode().value);
                std::lock_guard cb_lock(m_mutex);
                if (m_callbacks.on_indication_result) {
                    m_callbacks.on_indication_result(to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(),
                                                     false, -1, async_op.ErrorCode().value, m_callbacks.user_data);
                }
            }
        });
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION: " << ex.message());
    } catch (...) {
        WIN_LOG(L"SOMETHING WENT WRONG");
    }
}

void ble_advertiser::respond_read(BLERequestHandle request, const uint8_t* data, const size_t len,
                                  const int32_t status) {
    WIN_LOG(L"RESPONDING TO READ REQUEST. STATUS: " << status);
    auto* req_ctx = static_cast<BLERequestContext*>(request);
    if (!req_ctx) return;

    if (const auto* read = std::get_if<BLERequestContext::Read>(&req_ctx->data)) {
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
    WIN_LOG(L"RESPONDING TO WRITE REQUEST. STATUS: " << status);
    auto* req_ctx = static_cast<BLERequestContext*>(request);
    if (!req_ctx) return;

    if (const auto* write = std::get_if<BLERequestContext::Write>(&req_ctx->data)) {
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
