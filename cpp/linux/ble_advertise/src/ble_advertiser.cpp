#include <adapter.h>
#include <advertisement.h>
#include <agent.h>
#include <application.h>
#include <device.h>
#include <glib.h>

#include "ble_advertiser.h"
#include "bt_utils.h"

ble_advertiser::ble_advertiser() {
    std::lock_guard lock(m_mutex);

    GError* error        = nullptr;
    auto* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
    if (dbusConnection == nullptr) {
        const std::string errormessage = "UNABLE TO CONNECT TO THE SYSTEM BUS";
        if (error != nullptr) throw std::runtime_error(errormessage + error->message);
        throw std::runtime_error(errormessage);
    }

    m_adapter = binc_adapter_get_default(m_dbus_connection);
    g_object_unref(dbusConnection);

    if (!m_adapter) throw std::runtime_error("CANNOT READ BLUETOOTH ADAPTER");

    LINUX_LOG("BT ADAPTER FOUND");
    m_app = binc_create_application(m_adapter);

    if (m_app == nullptr) throw std::runtime_error("CANNOT CREATE AN APP INSTANCE");

    // All the callback bridges are registered
    binc_application_set_char_read_cb(m_app, &ble_advertiser::on_char_read_cb);
    binc_application_set_char_write_cb(m_app, &ble_advertiser::on_char_write_cb);
    binc_application_set_desc_read_cb(m_app, &ble_advertiser::on_desc_read_cb);
    binc_application_set_desc_write_cb(m_app, &ble_advertiser::on_desc_write_cb);
    binc_application_set_char_updated_cb(m_app, &ble_advertiser::on_char_updated);
}

ble_advertiser::~ble_advertiser() {
    stop();

    std::lock_guard lock(m_mutex);
    if (m_app) {
        LINUX_LOG("CLEARING APPLICATION");
        binc_application_free(m_app);
        m_app = nullptr;
    }
    if (m_agent) {
        LINUX_LOG("CLEARING AGENT");
        binc_agent_free(m_agent);
        m_agent = nullptr;
    }
}

void ble_advertiser::register_callbacks(const BLEAdvertiserCallbacks& callbacks) {
    std::lock_guard lock(m_mutex);
    m_callbacks = callbacks;
}

int32_t ble_advertiser::get_status() const {
    std::lock_guard lock(m_mutex);
    if (!m_adapter) {
        LINUX_LOG("BT ADAPTER IS NOT SET");
        return -1;
    }
    return m_is_advertising ? 1 : 0;
}

void ble_advertiser::add_service(const char* service_uuid) {
    std::lock_guard lock(m_mutex);
    if (!m_app || !service_uuid) {
        LINUX_LOG("BT ADAPTER IS NOT SET OR MISSING SERVICE UUID");
        return;
    }
    binc_application_add_service(m_app, service_uuid);

    if (m_primary_service_uuid.empty()) {
        m_primary_service_uuid = std::string(service_uuid);
    }
}

void ble_advertiser::add_characteristic(const ble_characteristics characteristics) const {
    std::lock_guard lock(m_mutex);
    if (!m_app) return;

    if (m_primary_service_uuid.empty()) {
        LINUX_LOG("MISSING PRIMARY SERVICE SET SERVICE FIRST");
        return;
    }

    uint8_t properties = 0;
    if (characteristics.can_read) properties |= GATT_CHR_PROP_READ;
    if (characteristics.can_write) properties |= GATT_CHR_PROP_WRITE;
    if (characteristics.can_write_no_response) properties |= GATT_CHR_PROP_WRITE_WITHOUT_RESP;
    if (characteristics.can_notify) properties |= GATT_CHR_PROP_NOTIFY;
    if (characteristics.can_indicate) properties |= GATT_CHR_PROP_INDICATE;

    binc_application_add_characteristic(m_app, m_primary_service_uuid.c_str(), characteristics.characteristic_uuid,
                                        properties);
}

void ble_advertiser::add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid) const {
    std::lock_guard lock(m_mutex);
    if (!m_app || m_primary_service_uuid.empty()) return;
    constexpr uint8_t permissions = GATT_CHR_PROP_READ | GATT_CHR_PROP_WRITE;
    binc_application_add_descriptor(m_app, m_primary_service_uuid.c_str(), characteristic_uuid, descriptor_uuid,
                                    permissions);
}

void ble_advertiser::start(const BLEAdvertiseConfig& config) {
    std::lock_guard lock(m_mutex);
    if (!m_adapter || !m_app || m_is_advertising) return;

    binc_adapter_register_application(m_adapter, m_app);

    if (m_advertisement) {
        LINUX_LOG("ADVERTISEMENT ALREADY PRESENT CLEARING IT");
        binc_advertisement_free(m_advertisement);
        m_advertisement = nullptr;
    }

    m_advertisement = binc_advertisement_create();
    binc_advertisement_set_secondary_channel(m_advertisement, BINC_SC_1M);
    binc_advertisement_set_general_discoverable(m_advertisement, config.discoverable);

    if (config.service_data && config.service_data_len > 0) {
        GByteArray* service_data_bytes = g_byte_array_sized_new(config.service_data_len);
        g_byte_array_append(service_data_bytes, config.service_data, config.service_data_len);
        binc_advertisement_set_service_data(m_advertisement, m_primary_service_uuid.c_str(), service_data_bytes);
        g_byte_array_free(service_data_bytes, TRUE);
    }

    binc_adapter_start_advertising(m_adapter, m_advertisement);
    m_is_advertising = true;
}

void ble_advertiser::stop() {
    std::lock_guard lock(m_mutex);
    if (!m_is_advertising) return;

    if (m_adapter && m_advertisement) {
        binc_adapter_stop_advertising(m_adapter, m_advertisement);
        binc_advertisement_free(m_advertisement);
        m_advertisement = nullptr;
    }

    if (m_adapter && m_app) {
        binc_adapter_unregister_application(m_adapter, m_app);
    }
    m_is_advertising = false;
}

bool ble_advertiser::send_notification(const char* device_address, const char* characteristic_uuid,
                                       const uint8_t* value, size_t value_len) const {
    std::lock_guard lock(m_mutex);
    if (!m_app || !m_is_advertising || m_primary_service_uuid.empty()) return false;

    GByteArray* byteArray = g_byte_array_sized_new(value_len);
    g_byte_array_append(byteArray, value, value_len);

    binc_application_notify(m_app, m_primary_service_uuid.c_str(), characteristic_uuid, byteArray);

    g_byte_array_free(byteArray, TRUE);
    return true;
}

void ble_advertiser::respond_read(std::unique_ptr<BLERequestContext> request, const uint8_t* data, size_t len,
                                  int32_t status) {
    if (!request) return;

    if (const auto* read_req = std::get_if<BLERequestContext::Read>(&request->data);
        read_req && status == 0 && data && len > 0) {
        GByteArray* byteArray = g_byte_array_sized_new(len);
        g_byte_array_append(byteArray, data, len);

        binc_application_set_char_value(request->app_ref, read_req->service_uuid.c_str(), read_req->char_uuid.c_str(),
                                        byteArray);

        g_byte_array_free(byteArray, TRUE);
    }
    request->complete();
}

void ble_advertiser::respond_write(std::unique_ptr<BLERequestContext> request, int32_t status) {
    if (!request) return;
    request->complete();
}

const char* ble_advertiser::on_char_read_cb(const Application* app, const char* address, const char* service_uuid,
                                            const char* char_uuid, guint16 mtu, guint16 offset) {
    if (!s_instance) return BLUEZ_ERROR_FAILED;

    std::lock_guard lock(s_instance->m_mutex);
    if (s_instance->m_callbacks.on_read_characteristic) {
        auto req_ctx =
            std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "", char_uuid ? char_uuid : "");

        s_instance->m_callbacks.on_read_characteristic(req_ctx.release(), address, service_uuid, char_uuid, 0,
                                                       s_instance->m_callbacks.user_data);
    }

    return nullptr; // Return NULL to signal DBus success
}

const char* ble_advertiser::on_char_write_cb(const Application* app, const char* address, const char* service_uuid,
                                             const char* char_uuid, GByteArray* byteArray, guint16 mtu,
                                             guint16 offset) {
    if (!s_instance) return BLUEZ_ERROR_FAILED;

    std::lock_guard lock(s_instance->m_mutex);
    if (s_instance->m_callbacks.on_write_characteristic) {
        std::vector<uint8_t> val_vec;
        if (byteArray && byteArray->len > 0) {
            val_vec.assign(byteArray->data, byteArray->data + byteArray->len);
        }

        auto req_ctx = std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "",
                                                           char_uuid ? char_uuid : "", val_vec);

        s_instance->m_callbacks.on_write_characteristic(req_ctx.release(), address, service_uuid, char_uuid,
                                                        val_vec.data(), val_vec.size(), false,
                                                        s_instance->m_callbacks.user_data);
    }

    return nullptr;
}
const char* ble_advertiser::on_desc_read_cb(const Application* application, const char* address,
                                            const char* service_uuid, const char* char_uuid, const char* desc_uuid) {
    return nullptr;
}
const char* ble_advertiser::on_desc_write_cb(const Application* application, const char* address,
                                             const char* service_uuid, const char* char_uuid, const char* desc_uuid,
                                             const GByteArray* byteArray) {
    return nullptr;
}

void ble_advertiser::on_char_updated(const Application* application, const char* service_uuid, const char* char_uuid,
                                     GByteArray* byteArray) {}
