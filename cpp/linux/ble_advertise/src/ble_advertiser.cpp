#include <adapter.h>
#include <advertisement.h>
#include <agent.h>
#include <application.h>
#include <device.h>
#include <glib.h>

#include "ble_advertiser.h"
#include "bt_utils.h"

ble_advertiser* ble_advertiser::s_instance = nullptr;
std::mutex ble_advertiser::s_instance_mutex{};

ble_advertiser::ble_advertiser() {

    // Initialize GLib Main Context Loop on a background worker thread
    m_main_loop   = g_main_loop_new(nullptr, FALSE);
    m_glib_thread = std::thread([this]() { g_main_loop_run(m_main_loop); });

    {
        std::lock_guard lock(m_mutex);
        GError* error = nullptr;

        m_dbus_connection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
        if (m_dbus_connection == nullptr) {
            const std::string errormessage = "UNABLE TO CONNECT TO THE SYSTEM BUS: ";
            const std::string detail       = error ? error->message : "Unknown error";
            if (error) g_error_free(error);
            throw std::runtime_error(errormessage + detail);
        }

        m_adapter = binc_adapter_get_default(m_dbus_connection);

        if (!m_adapter) throw std::runtime_error("CANNOT READ BLUETOOTH ADAPTER");

        LINUX_LOG("BT ADAPTER FOUND");
        m_app = binc_create_application(m_adapter);

        if (m_app == nullptr) throw std::runtime_error("CANNOT CREATE AN APP INSTANCE");
        LINUX_LOG("CONNECTOR APPLICATION CREATED");

        m_is_secondary_channel_supported = utils::is_secondary_channel_supported(m_dbus_connection, m_adapter);
        max_advertising_length           = utils::get_max_adv_length(m_dbus_connection, m_adapter);
    }

    {
        std::lock_guard inst_lock(s_instance_mutex);
        s_instance = this;
    }

    // Callback setup
    binc_application_set_char_read_cb(
        m_app,
        [](const Application* app, const char* address, const char* service_uuid, const char* char_uuid, guint16,
           guint16) -> const char* {
            BLEAdvertiserCallbacks callback_ref;
            {
                std::lock_guard inst_lock(s_instance_mutex);
                if (!s_instance) return BLUEZ_ERROR_FAILED;

                std::lock_guard lock(s_instance->m_mutex);
                if (s_instance->m_callbacks.on_read_characteristic == nullptr) return BLUEZ_ERROR_FAILED;
                callback_ref = s_instance->m_callbacks;
            }

            LINUX_LOG("READ REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "")
                                                      << " CHARACTERISTICS ID  : " << (char_uuid ? char_uuid : ""));

            auto req_ctx =
                std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "", char_uuid ? char_uuid : "");

            if (callback_ref.on_read_characteristic == nullptr) return nullptr;
            callback_ref.on_read_characteristic(req_ctx.release(), address, service_uuid, char_uuid, 0,
                                                callback_ref.user_data);
            return nullptr;
        });

    binc_application_set_char_write_cb(
        m_app,
        [](const Application* app, const char* address, const char* service_uuid, const char* char_uuid,
           GByteArray* byteArray, const guint16, const guint16) -> const char* {
            BLEAdvertiserCallbacks callback_ref;

            {
                std::lock_guard inst_lock(s_instance_mutex);
                if (!s_instance) return BLUEZ_ERROR_REJECTED;

                std::lock_guard lock(s_instance->m_mutex);
                if (s_instance->m_callbacks.on_write_characteristic == nullptr) return BLUEZ_ERROR_REJECTED;
                callback_ref = s_instance->m_callbacks;
            }

            LINUX_LOG("WRITE REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "")
                                                       << " CHARACTERISTICS ID  : " << (char_uuid ? char_uuid : ""));

            std::vector<uint8_t> value_vector;
            if (byteArray && byteArray->len > 0) value_vector.assign(byteArray->data, byteArray->data + byteArray->len);

            auto req_ctx = std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "",
                                                               char_uuid ? char_uuid : "", value_vector);

            // Call outside mutex lock
            if (callback_ref.on_write_characteristic == nullptr) return nullptr;
            callback_ref.on_write_characteristic(req_ctx.release(), address, service_uuid, char_uuid,
                                                 value_vector.data(), value_vector.size(), false,
                                                 callback_ref.user_data);
            return nullptr;
        });

    binc_application_set_desc_read_cb(
        m_app,
        [](const Application* app, const char* address, const char* service_uuid, const char* char_uuid,
           const char* desc_uuid) -> const char* {
            BLEAdvertiserCallbacks callback_ref;
            {
                std::lock_guard inst_lock(s_instance_mutex);
                if (!s_instance) return BLUEZ_ERROR_FAILED;

                std::lock_guard lock(s_instance->m_mutex);
                if (s_instance->m_callbacks.on_read_descriptor == nullptr) return BLUEZ_ERROR_FAILED;
                callback_ref = s_instance->m_callbacks;
            }

            LINUX_LOG("READ REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "")
                                                      << " CHARACTERISTICS ID  : " << (char_uuid ? char_uuid : "")
                                                      << "DESCRIPTOR ID" << (desc_uuid ? desc_uuid : ""));

            auto req_ctx =
                std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "", char_uuid ? char_uuid : "");

            if (callback_ref.on_read_descriptor == nullptr) return nullptr;
            callback_ref.on_read_descriptor(req_ctx.release(), address, service_uuid, char_uuid, desc_uuid, 0,
                                            callback_ref.user_data);
            return nullptr;
        });

    binc_application_set_desc_write_cb(
        m_app,
        [](const Application* app, const char* address, const char* service_uuid, const char* char_uuid,
           const char* desc_uuid, const GByteArray* byteArray) -> const char* {
            BLEAdvertiserCallbacks callback_ref;

            {
                std::lock_guard inst_lock(s_instance_mutex);
                if (!s_instance) return BLUEZ_ERROR_REJECTED;

                std::lock_guard lock(s_instance->m_mutex);
                if (s_instance->m_callbacks.on_write_descriptor == nullptr) return BLUEZ_ERROR_REJECTED;
                callback_ref = s_instance->m_callbacks;
            }

            LINUX_LOG("WRITE REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "")
                                                       << " CHARACTERISTICS ID  : " << (char_uuid ? char_uuid : "")
                                                       << "DESCRIPTOR ID" << (desc_uuid ? desc_uuid : ""));

            std::vector<uint8_t> value_vector;
            if (byteArray && byteArray->len > 0) value_vector.assign(byteArray->data, byteArray->data + byteArray->len);

            auto req_ctx = std::make_unique<BLERequestContext>(app, service_uuid ? service_uuid : "",
                                                               char_uuid ? char_uuid : "", value_vector);

            if (callback_ref.on_write_descriptor == nullptr) return nullptr;
            callback_ref.on_write_descriptor(req_ctx.release(), address, service_uuid, char_uuid, desc_uuid,
                                             value_vector.data(), value_vector.size(), false, callback_ref.user_data);
            return nullptr;
        });

    binc_application_set_char_start_notify_cb(m_app, [](const Application* app, const char* service_uuid,
                                                        const char* char_uuid) {
        LINUX_LOG("NOTIFY REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "") << " CHARACTERISTICS ID  : "
                                                    << (char_uuid ? char_uuid : "") << "STARTED");
    });

    binc_application_set_char_stop_notify_cb(m_app, [](const Application* app, const char* service_uuid,
                                                       const char* char_uuid) {
        LINUX_LOG("NOTIFY REQUEST ON SERVICE ID : " << (service_uuid ? service_uuid : "") << " CHARACTERISTICS ID  : "
                                                    << (char_uuid ? char_uuid : "") << "STOPPED");
    });

    binc_adapter_register_application(m_adapter, m_app);
}

ble_advertiser::~ble_advertiser() {
    stop();

    std::lock_guard lock(m_mutex);

    {
        std::lock_guard inst_lock(s_instance_mutex);
        if (s_instance == this) {
            LINUX_LOG("STATIC REFERENCES REMOVED");
            s_instance = nullptr;
        }
    }

    if (m_app) {
        LINUX_LOG("CLEARING APPLICATION");
        binc_application_free(m_app);
        m_app = nullptr;
    }
    if (m_agent) {
        LINUX_LOG("CLEARING CONNECTION AGENT");
        binc_agent_free(m_agent);
        m_agent = nullptr;
    }
    if (m_dbus_connection) {
        LINUX_LOG("CLEARING DBUS CONNECTION");
        g_object_unref(m_dbus_connection);
        m_dbus_connection = nullptr;
    }

    if (m_main_loop) {
        // Clean up GLib Main Loop Thread
        g_main_loop_quit(m_main_loop);
        if (m_glib_thread.joinable()) m_glib_thread.join();
        g_main_loop_unref(m_main_loop);
        m_main_loop = nullptr;
    }
}

void ble_advertiser::register_callbacks(const BLEAdvertiserCallbacks& callbacks) {
    std::lock_guard lock(m_mutex);
    LINUX_LOG("CALLBACKS REGISTERED SUCCESSFULLY");
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
    if (!m_app || !service_uuid) {
        LINUX_LOG("BT ADAPTER IS NOT SET OR MISSING SERVICE UUID");
        return;
    }
    LINUX_LOG("ADDING SERVICE ID ");
    const auto service_id_lowercase = utils::to_lower_uuid(service_uuid);
    LINUX_LOG("ADDING SERVICE ID " << service_id_lowercase);

    {
        std::lock_guard lock(m_mutex);
        binc_application_add_service(m_app, service_id_lowercase.c_str());
        LINUX_LOG("SERVICE ADDED SUCCESSFULLY");

        if (m_primary_service_uuid.empty()) {
            m_primary_service_uuid = service_id_lowercase;
        }
    }

    if (m_callbacks.on_service_added == nullptr) return;

    LINUX_LOG("ON SERVICE ADDED SUCCESSFULL");
    m_callbacks.on_service_added(service_id_lowercase.c_str(), 0, m_callbacks.user_data);
}

void ble_advertiser::add_characteristic(ble_characteristics characteristics) const {
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

    const auto char_uuid_lowercase = utils::to_lower_uuid(characteristics.characteristic_uuid);
    binc_application_add_characteristic(m_app, m_primary_service_uuid.c_str(), char_uuid_lowercase.c_str(), properties);
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

    if (m_advertisement) {
        LINUX_LOG("ADVERTISEMENT ALREADY PRESENT CLEARING IT");
        binc_advertisement_free(m_advertisement);
        m_advertisement = nullptr;
    }

    m_advertisement = binc_advertisement_create();
    binc_advertisement_set_general_discoverable(m_advertisement, config.discoverable);

    if (m_is_secondary_channel_supported) {
        LINUX_LOG("SECONDARY CHANNEL SUPPORTED");
        binc_advertisement_set_secondary_channel(m_advertisement, BINC_SC_1M);
    }

    if (config.service_data && config.service_data_len > 0) {
        const size_t uuid_bytes             = (m_primary_service_uuid.length() > 8) ? 16 : 2;
        const size_t service_data_total_len = 2 + uuid_bytes + config.service_data_len;
        const size_t total_adv_len          = service_data_total_len + (config.discoverable ? 3 : 0);

        LINUX_LOG("CALCULATED TOTAL ADV PACKET SIZE: " << total_adv_len << " / MAX ALLOWED: "
                                                       << static_cast<uint>(max_advertising_length) << " BYTES");

        if (total_adv_len <= max_advertising_length) {
            GByteArray* service_data_bytes = g_byte_array_sized_new(config.service_data_len);
            g_byte_array_append(service_data_bytes, config.service_data, config.service_data_len);
            binc_advertisement_set_service_data(m_advertisement, m_primary_service_uuid.c_str(), service_data_bytes);
            g_byte_array_free(service_data_bytes, TRUE);
            LINUX_LOG("ADVERTISEMENT DATA SET SUCCESSFULLY");
        } else {
            LINUX_LOG("ERROR: TOTAL ADVERTISEMENT DATA EXCEEDS LIMIT (" << total_adv_len << " > "
                                                                        << static_cast<uint>(max_advertising_length)
                                                                        << " BYTES). SKIPPING SERVICE DATA.");
            throw std::runtime_error("FAILED TO SET ADVERTISEMENT SIZE");
        }
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

    if (const auto* read_req = std::get_if<BLERequestContext::ReadCharacteristics>(&request->data);
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