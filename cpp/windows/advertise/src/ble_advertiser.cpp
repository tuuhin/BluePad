#include <iostream>
#include <map>
#include <memory>
#include <mutex>
#include <plog/Log.h>
#include <string>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Storage.Streams.h>

#include "ble_advertiser.h"
#include "utils.h"

using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Foundation;

namespace {
hstring guuid_to_hstring(guid const& g) {
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

hstring get_remote_device_address(hstring const& winString) {
    const std::wstring_view strView{winString};

    if (strView.length() < 17) return L"";
    std::wstring mac{strView.substr(strView.length() - 17)};

    // Standardize to uppercase (convention for MAC addresses)
    std::ranges::transform(mac, mac.begin(), [](const wchar_t c) { return towupper(c); });

    return hstring{mac};
}
} // namespace

ble_advertiser::ble_advertiser() {
    init_apartment();
    WIN_LOG(L"BLE ADVERTISER INITIALIZED");
}

ble_advertiser::~ble_advertiser() {
    std::lock_guard lock(m_mutex);
    if (m_service_provider == nullptr) {
        WIN_LOG(L"SERVICE PROVIDER ALREADY DESTROYED");
        return;
    }
    try {
        const auto status = m_service_provider.AdvertisementStatus();
        if (status == GattServiceProviderAdvertisementStatus::Started ||
            status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
            WIN_LOG(L"BLE ADVERTISER STATUS WAS " << log_advertisement_status(status));
            m_service_provider.StopAdvertising();
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN DESTRUCTOR: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
    WIN_LOG(L"BLE ADVERTISER DESTRUCTOR CALLED");
    m_service_provider = nullptr;
}

void ble_advertiser::register_callbacks(const BLEAdvertiserCallbacks& callbacks) {
    std::lock_guard lock(m_mutex);
    m_callbacks = callbacks;
    WIN_LOG(L"BLE ADVERTISEMENT CALLBACK CREATED");
}

int32_t ble_advertiser::get_status() const {
    std::lock_guard lock(m_mutex);
    if (m_service_provider == nullptr) return -1;
    return static_cast<int32_t>(m_service_provider.AdvertisementStatus());
}

void ble_advertiser::start(const BLEAdvertiseConfig& config) const {
    std::lock_guard lock(m_mutex);
    if (m_service_provider == nullptr) {
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
        WIN_LOG(L"ADVERTISEMENT STATUS AFTER START CALLED " << log_advertisement_status(status));

    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN START: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
        WIN_LOG(L"UNABLE TO START ADVERTISER");
    }
}

void ble_advertiser::stop() const {

    std::lock_guard lock(m_mutex);
    if (m_service_provider == nullptr) return;

    try {
        const auto status = m_service_provider.AdvertisementStatus();
        WIN_LOG(L"ADVERTISEMENT STATUS BEFORE STOP REQUESTED : " << log_advertisement_status(status));

        if (status == GattServiceProviderAdvertisementStatus::Started ||
            status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
            m_service_provider.StopAdvertising();

            const auto status2 = m_service_provider.AdvertisementStatus();
            WIN_LOG(L"ADVERTISEMENT STATUS AFTER STOP ADVERTISING : " << log_advertisement_status(status2));
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN STOP: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
        WIN_LOG(L"UNABLE TO STOP ADVERTISER");
    }
}

void ble_advertiser::add_service(const char* service_uuid) {
    try {
        const hstring h_uuid = to_hstring(service_uuid);
        WIN_LOG(L"ADDING SERVICE: " << h_uuid.c_str());

        const guid service_guid{h_uuid};
        const auto result                    = GattServiceProvider::CreateAsync(service_guid).get();
        OnServiceAddedCallback callbacks_ref = nullptr;
        void* user_data                      = nullptr;

        {
            std::lock_guard lock(m_mutex);
            user_data     = m_callbacks.user_data;
            callbacks_ref = m_callbacks.on_service_added;
        }

        if (callbacks_ref == nullptr || user_data == nullptr) return;
        callbacks_ref(service_uuid, static_cast<int32_t>(result.Error()), user_data);

        if (result.Error() != BluetoothError::Success) {
            WIN_LOG(L"FAILED TO ADD SERVICE. ERROR: " << static_cast<int32_t>(result.Error()));
            return;
        }
        {
            std::lock_guard lock(m_mutex);
            m_service_provider = result.ServiceProvider();
            WIN_LOG(L"SERVICE ADDED SUCCESSFULLY SERVICE ID: " << h_uuid.c_str());
        }

        WIN_LOG(L"WILL RESPONSE TO ADVERTISEMENT STATUS CHANGES");
        m_service_provider.AdvertisementStatusChanged(
            [weak_self = weak_from_this()](GattServiceProvider const&,
                                           GattServiceProviderAdvertisementStatusChangedEventArgs const& args) {
                WIN_LOG(L"ADVERTISEMENT STATUS CHANGED : " << log_advertisement_status(args.Status()));

                auto const self = weak_self.lock();
                if (self == nullptr) return;

                OnServiceStatusChangeCallback callback_ref = nullptr;
                void* user_callback_data                   = nullptr;
                {
                    std::lock_guard lock(self->m_mutex);
                    callback_ref       = self->m_callbacks.on_service_status_change;
                    user_callback_data = self->m_callbacks.user_data;
                }

                if (callback_ref == nullptr || user_callback_data == nullptr) {
                    WIN_LOG(L"UNABLE TO SET CALLBACK");
                    return;
                }
                callback_ref(static_cast<int32_t>(args.Status()), user_callback_data);
            });
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION IN ADD_SERVICE: " << ex.message().c_str());
    } catch (std::exception const& std_ex) {
        WIN_LOG(L"STANDARD C++ EXCEPTION IN ADD_SERVICE" << std_ex.what());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION IN ADD_SERVICE");
    }
}

void ble_advertiser::add_characteristic(const ble_characteristics characteristics) {

    GattServiceProvider provider = nullptr;
    {
        std::lock_guard lock(m_mutex);
        if (m_service_provider == nullptr) {
            WIN_LOG(L"SERVICE PROVIDER NOT INITIALIZED, CANNOT ADD CHARACTERISTIC");
            return;
        }
        provider = m_service_provider;
    }
    try {
        const hstring h_uuid = to_hstring(characteristics.characteristic_uuid);
        WIN_LOG(L"ADDING CHARACTERISTIC: " << h_uuid.c_str());

        const GattLocalCharacteristicParameters params;
        auto properties = GattCharacteristicProperties::None;

        if (characteristics.can_read) {
            properties |= GattCharacteristicProperties::Read;
            params.ReadProtectionLevel(GattProtectionLevel::EncryptionRequired);
        }
        if (characteristics.can_write) {
            properties |= GattCharacteristicProperties::Write;
            params.WriteProtectionLevel(GattProtectionLevel::EncryptionAndAuthenticationRequired);
        }
        if (characteristics.can_write_no_response) {
            properties |= GattCharacteristicProperties::WriteWithoutResponse;
            params.WriteProtectionLevel(GattProtectionLevel::EncryptionAndAuthenticationRequired);
        }
        if (characteristics.can_notify) properties |= GattCharacteristicProperties::Notify;
        if (characteristics.can_indicate) properties |= GattCharacteristicProperties::Indicate;

        params.CharacteristicProperties(properties);

        const auto result = m_service_provider.Service().CreateCharacteristicAsync(guid(h_uuid), params).get();
        if (result.Error() != BluetoothError::Success) {
            WIN_LOG(L"FAILED TO ADD CHARACTERISTIC. ERROR: " << static_cast<int32_t>(result.Error()));
            return;
        }
        const auto characteristic = result.Characteristic();
        WIN_LOG(L"CHARACTERISTIC ADDED SUCCESSFULLY UUID: " << h_uuid.c_str());

        hstring w_service_uuid = guuid_to_hstring(m_service_provider.Service().Uuid());

        {
            std::lock_guard lock(m_mutex);
            m_characteristics.insert(std::make_pair(h_uuid, characteristic));

            characteristic.ReadRequested([weak_self = weak_from_this(), w_service_uuid, h_uuid](
                                             GattLocalCharacteristic const&, GattReadRequestedEventArgs const& args) {
                try {
                    const auto self = weak_self.lock();
                    if (self == nullptr) return;

                    WIN_LOG(L"CHARACTERISTIC READ REQUESTED. UUID: " << h_uuid.c_str());
                    const auto deferral  = args.GetDeferral();
                    const auto request   = args.GetRequestAsync().get();
                    const auto device_id = args.Session().DeviceId().Id();

                    OnReadCharacteristicCallback callbacks_ref = nullptr;
                    void* user_data                            = nullptr;
                    {
                        std::lock_guard callback_lock(self->m_mutex);
                        callbacks_ref = self->m_callbacks.on_read_characteristic;
                        user_data     = self->m_callbacks.user_data;
                    }

                    if (callbacks_ref == nullptr || user_data == nullptr) {
                        WIN_LOG(L"NO READ CALLBACK REGISTERED FOR CHARACTERISTIC");
                        request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                        return;
                    }

                    auto req_ctx       = std::make_unique<BLERequestContext>(args, request, deferral);
                    const auto raw_ptr = req_ctx.release();
                    callbacks_ref(reinterpret_cast<BLERequestHandle>(raw_ptr), to_string(device_id).c_str(),
                                  to_string(w_service_uuid).c_str(), to_string(h_uuid).c_str(), 0, user_data);
                } catch (...) {
                    WIN_LOG(L"EXCEPTION IN READ REQUESTED HANDLER");
                }
            });

            characteristic.WriteRequested([weak_self = weak_from_this(), w_service_uuid, h_uuid](
                                              GattLocalCharacteristic const&, GattWriteRequestedEventArgs const& args) {
                try {
                    const auto self = weak_self.lock();
                    if (self == nullptr) return;

                    const auto request                   = args.GetRequestAsync().get();
                    const bool is_write_without_response = (request.Option() == GattWriteOption::WriteWithoutResponse);

                    const wchar_t* write_type =
                        is_write_without_response ? L"WRITE WITHOUT RESPONSE" : L" WRITE WITH RESPONSE";

                    WIN_LOG(L"CHARACTERISTIC WRITE REQUESTED. UUID: " << h_uuid.c_str() << L" PROPERTY: "
                                                                      << write_type);
                    const auto deferral  = args.GetDeferral();
                    const auto device_id = args.Session().DeviceId().Id();
                    const auto buffer    = request.Value();

                    OnWriteCharacteristicCallback callback_ref = nullptr;
                    void* user_data                            = nullptr;
                    {
                        std::lock_guard callback_lock(self->m_mutex);
                        callback_ref = self->m_callbacks.on_write_characteristic;
                        user_data    = self->m_callbacks.user_data;
                    }

                    if (callback_ref == nullptr || nullptr) {
                        WIN_LOG(L"NO WRITE CALLBACK REGISTERED FOR CHARACTERISTIC");
                        return;
                    }

                    auto req_ctx       = std::make_unique<BLERequestContext>(args, request, deferral);
                    const auto raw_ptr = req_ctx.release();
                    callback_ref(reinterpret_cast<BLERequestHandle>(raw_ptr), to_string(device_id).c_str(),
                                 to_string(w_service_uuid).c_str(), to_string(h_uuid).c_str(), buffer.data(),
                                 buffer.Length(), is_write_without_response, user_data);
                } catch (...) {
                    WIN_LOG(L"EXCEPTION IN WRITE REQUESTED HANDLER");
                }
            });
        }

        const auto characteristics_properties = characteristic.CharacteristicProperties();
        const auto isNotify =
            (characteristics_properties & GattCharacteristicProperties::Notify) != GattCharacteristicProperties::None;
        const auto isIndicate =
            (characteristics_properties & GattCharacteristicProperties::Indicate) != GattCharacteristicProperties::None;
        if (!isNotify && !isIndicate) return;

        characteristic.SubscribedClientsChanged(
            [weak_self = weak_from_this()](GattLocalCharacteristic const& ch, auto const&) {
                const auto clients                = ch.SubscribedClients();
                const auto characteristics_uuid_h = guuid_to_hstring(ch.Uuid());

                // 1. Determine the exact type string for cleaner logs
                const bool is_indication = (ch.CharacteristicProperties() & GattCharacteristicProperties::Indicate) ==
                                           GattCharacteristicProperties::Indicate;
                const wchar_t* sub_type = is_indication ? L"INDICATION" : L"NOTIFICATION";

                WIN_LOG(L"--- SUBSCRIBED CLIENTS CHANGED ---");
                WIN_LOG(L"CHARACTERISTICS UUID : " << characteristics_uuid_h.c_str());
                WIN_LOG(L"SUBSCRIPTION TYPE   : " << sub_type);
                WIN_LOG(L"NO OF ACTIVE CLIENTS: " << clients.Size());

                // 2. Log each subscribed client cleanly
                for (GattSubscribedClient const& client : clients) {
                    auto address = client.Session().DeviceId().Id();
                    WIN_LOG(L"SUBSCRIBER ADDRESS: " << get_remote_device_address(address).c_str());
                }
                WIN_LOG(L"----------------------------------");
            });

    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION WITH ADDING CHARACTERISTICS HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                        << ex.message().c_str());
    } catch (std::exception const& ex) {
        WIN_LOG(L"STD EXCEPTION OCCURRED :" << ex.what());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION WHILE ADDING CHARACTERISTICS");
        utils::show_stacktrace();
    }
}

void ble_advertiser::add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid) {
    GattLocalCharacteristic characteristic = nullptr;
    hstring w_service_uuid;

    const std::wstring w_desc_uuid = to_hstring(descriptor_uuid).c_str();
    std::wstring w_char_uuid       = to_hstring(characteristic_uuid).c_str();

    {
        std::lock_guard lock(m_mutex);
        if (!m_service_provider) {
            WIN_LOG(L"SERVICE PROVIDER NOT INITIALIZED, CANNOT ADD DESCRIPTOR");
            return;
        }

        // Thread-safe map lookup
        const auto it = m_characteristics.find(w_char_uuid);
        if (it == m_characteristics.end()) {
            WIN_LOG(L"CHARACTERISTIC NOT FOUND, CANNOT ADD DESCRIPTOR");
            return;
        }

        characteristic = it->second;
        w_service_uuid = guuid_to_hstring(m_service_provider.Service().Uuid());
    }
    try {

        if (guid(w_desc_uuid) == GattDescriptorUuids::ClientCharacteristicConfiguration()) {
            WIN_LOG(L"CCC DESCRIPTOR (2902) IS MANAGED BY THE SYSTEM, CANNOT ADD MANUALLY");
            return;
        }
        const GattLocalDescriptorParameters params;
        params.ReadProtectionLevel(GattProtectionLevel::EncryptionRequired);
        params.WriteProtectionLevel(GattProtectionLevel::EncryptionAndAuthenticationRequired);

        {
            const auto result = characteristic.CreateDescriptorAsync(guid(w_desc_uuid), params).get();
            if (result.Error() != BluetoothError::Success) {
                WIN_LOG(L"FAILED TO ADD DESCRIPTOR. ERROR: " << static_cast<int32_t>(result.Error()));
                return;
            }
            const auto descriptor = result.Descriptor();
            WIN_LOG(L"DESCRIPTOR ADDED SUCCESSFULLY UUID: " << w_desc_uuid.c_str());
            std::lock_guard lock(m_mutex);

            descriptor.ReadRequested([weak_self = weak_from_this(), w_service_uuid, w_char_uuid,
                                      w_desc_uuid](GattLocalDescriptor const&, GattReadRequestedEventArgs const& args) {
                try {
                    const auto self = weak_self.lock();
                    if (self == nullptr) return;

                    WIN_LOG(L"DESCRIPTOR READ REQUESTED. UUID: " << w_desc_uuid.c_str());
                    const auto deferral  = args.GetDeferral();
                    const auto request   = args.GetRequestAsync().get();
                    const auto device_id = args.Session().DeviceId().Id();

                    OnReadDescriptorCallback callbacks_ref = nullptr;
                    void* user_data                        = nullptr;
                    {
                        std::lock_guard callback_lock(self->m_mutex);
                        callbacks_ref = self->m_callbacks.on_read_descriptor;
                        user_data     = self->m_callbacks.user_data;
                    }

                    if (callbacks_ref == nullptr || user_data == nullptr) {
                        WIN_LOG(L"NO READ CALLBACK REGISTERED FOR DESCRIPTOR");
                        request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                        return;
                    }

                    auto req_ctx       = std::make_unique<BLERequestContext>(args, request, deferral);
                    const auto raw_ptr = req_ctx.release();
                    callbacks_ref(reinterpret_cast<BLERequestHandle>(raw_ptr), to_string(device_id).c_str(),
                                  to_string(w_service_uuid).c_str(), to_string(w_char_uuid).c_str(),
                                  to_string(w_desc_uuid).c_str(), 0, user_data);
                } catch (...) {
                    WIN_LOG(L"EXCEPTION IN DESCRIPTOR READ HANDLER");
                }
            });

            descriptor.WriteRequested([weak_self = weak_from_this(), w_service_uuid, w_char_uuid,
                                       w_desc_uuid](auto const&, GattWriteRequestedEventArgs const& args) {
                try {
                    const auto self = weak_self.lock();
                    if (self == nullptr) return;

                    WIN_LOG(L"DESCRIPTOR WRITE REQUESTED. UUID: " << w_desc_uuid.c_str());
                    const auto deferral  = args.GetDeferral();
                    const auto request   = args.GetRequestAsync().get();
                    const auto device_id = args.Session().DeviceId().Id();
                    const auto buffer    = request.Value();

                    OnWriteDescriptorCallback callback_ref = nullptr;
                    void* user_data                        = nullptr;

                    {
                        std::lock_guard callback_lock(self->m_mutex);
                        callback_ref = self->m_callbacks.on_write_descriptor;
                        user_data    = self->m_callbacks.user_data;
                    }

                    if (callback_ref == nullptr || user_data == nullptr) {
                        WIN_LOG(L"NO WRITE CALLBACK REGISTERED FOR DESCRIPTOR");
                        return;
                    }

                    auto req_ctx       = std::make_unique<BLERequestContext>(args, request, deferral);
                    const auto raw_ptr = req_ctx.release();
                    callback_ref(reinterpret_cast<BLERequestHandle>(raw_ptr), to_string(device_id).c_str(),
                                 to_string(w_service_uuid).c_str(), to_string(w_char_uuid).c_str(),
                                 to_string(w_desc_uuid).c_str(), buffer.data(), buffer.Length(), true, user_data);
                } catch (...) {
                    WIN_LOG(L"EXCEPTION IN DESCRIPTOR WRITE HANDLER");
                }
            });
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION WITH ADDING DESCRIPTOR HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                   << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION WHILE ADDING DESCRIPTOR");
        utils::show_stacktrace();
    }
}

bool ble_advertiser::send_notification(const char* device_address, const char* characteristic_uuid,
                                       const uint8_t* value, const size_t value_len) {
    WIN_LOG(L"SENDING NOTIFICATION TO: " << to_hstring(device_address).c_str() << L" CHAR: "
                                         << to_hstring(characteristic_uuid).c_str());

    std::wstring w_char_uuid               = to_hstring(characteristic_uuid).c_str();
    GattLocalCharacteristic characteristic = nullptr;

    {
        std::lock_guard lock(m_mutex);
        const auto matching_characteristic = m_characteristics.find(w_char_uuid);
        if (matching_characteristic == m_characteristics.end()) {
            WIN_LOG(L"CHARACTERISTIC NOT FOUND, CANNOT SEND NOTIFICATION");
            return false;
        }
        characteristic = matching_characteristic->second;
    }

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
        return false;
    }

    const auto operation     = characteristic.NotifyValueAsync(buffer, target_client);
    const bool is_indication = (characteristic.CharacteristicProperties() & GattCharacteristicProperties::Indicate) ==
                               GattCharacteristicProperties::Indicate;

    if (!is_indication) {
        WIN_LOG(L"CHARACTERISTICS DON'T HAVE INDICATE PROPERTIES SO WE DON'T CARE ABOUT THE ACK");
        return true;
    }

    try {
        operation.Completed(
            [weak_self = weak_from_this(), w_device_addr, w_char_uuid](auto const& async_op, AsyncStatus const status) {
                const auto self = weak_self.lock();
                if (self == nullptr) return;

                OnIndicationResultCallback cb = nullptr;
                void* cb_user_data            = nullptr;
                {
                    std::lock_guard callback_lock(self->m_mutex);
                    cb           = self->m_callbacks.on_indication_result;
                    cb_user_data = self->m_callbacks.user_data;
                }
                if (cb == nullptr) return;

                switch (status) {
                case AsyncStatus::Completed: {
                    auto result = async_op.GetResults();
                    WIN_LOG(L"INDICATION SENT SUCCESSFULLY. STATUS: " << static_cast<int32_t>(result.Status()));
                    cb(to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(), true,
                       static_cast<int32_t>(result.Status()), 0, cb_user_data);
                    break;
                }
                case AsyncStatus::Error: {
                    WIN_LOG(L"FAILED TO SEND INDICATION. ERROR: " << async_op.ErrorCode().value);
                    cb(to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(), false, -1,
                       async_op.ErrorCode().value, cb_user_data);
                    break;
                }

                case AsyncStatus::Canceled: {
                    WIN_LOG(L"INDICATION CANCELED");
                    cb(to_string(w_device_addr).c_str(), to_string(w_char_uuid).c_str(), false, -1,
                       async_op.ErrorCode().value, cb_user_data);
                } break;
                default:
                    break;
                }
            });
        return true;
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WINRT EXCEPTION WITH SENDING NOTIFICATION HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                      << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION WHILE SENDING NOTIFICATION");
        utils::show_stacktrace();
    }

    return false;
}

void ble_advertiser::respond_read(std::unique_ptr<BLERequestContext> request, const uint8_t* data, const size_t len,
                                  const int32_t status) {
    if (!request) {
        WIN_LOG("FAILED TO PROVIDE RESPOND READ CONTEXT");
        return;
    }

    const auto* read = std::get_if<BLERequestContext::Read>(&request->data);
    if (read == nullptr) return;

    if (status == 0) {
        const DataWriter writer;
        writer.WriteBytes(array_view(data, data + len));
        read->request.RespondWithValue(writer.DetachBuffer());
    } else {
        read->request.RespondWithProtocolError(static_cast<uint8_t>(status));
    }
    request->complete();
    WIN_LOG(L"RESPONDING TO READ REQUEST CLEARING DEFERRAL AND REQ CONTEXT");
}

void ble_advertiser::respond_write(std::unique_ptr<BLERequestContext> request, const int32_t status) {
    if (!request) {
        WIN_LOG(L"FAILED TO PROVIDE RESPOND WRITE CONTEXT");
        return;
    }

    const auto* write = std::get_if<BLERequestContext::Write>(&request->data);
    if (write == nullptr) return;

    if (write->request.Option() != GattWriteOption::WriteWithResponse) {
        WIN_LOG(L"WRITE WITHOUT RESPONSE ");
        request->complete();
        return;
    }

    if (status == 0)
        write->request.Respond();
    else
        write->request.RespondWithProtocolError(static_cast<uint8_t>(status));

    request->complete();
    WIN_LOG(L"RESPONDING TO WRITE REQUEST CLEARING DEFERRAL AND REQ OBJECT");
}
