#ifndef LINUX_BLE_BLE_ADVERTISE_H
#define LINUX_BLE_BLE_ADVERTISE_H

#include <glib.h>
#include <memory>
#include <mutex>
#include <string>
#include <variant>
#include <vector>

#include "ble_advertise_c.h"

struct BLERequestContext {
    struct Read {
        std::string service_uuid;
        std::string char_uuid;
    };

    struct Write {
        std::string service_uuid;
        std::string char_uuid;
        std::vector<uint8_t> value;
    };

    std::variant<Read, Write> data;
    const Application* app_ref = nullptr;
    bool completed             = false;

    BLERequestContext(const Application* app, std::string service_uuid, std::string char_uuid)
        : data(Read{std::move(service_uuid), std::move(char_uuid)}), app_ref(app) {}

    BLERequestContext(const Application* app, std::string service_uuid, std::string char_uuid, std::vector<uint8_t> val)
        : data(Write{std::move(service_uuid), std::move(char_uuid), std::move(val)}), app_ref(app) {}

    void complete() {
        if (completed) return;
        completed = true;
    }

    ~BLERequestContext() { complete(); }
};

class ble_advertiser : public std::enable_shared_from_this<ble_advertiser> {
public:
    ble_advertiser();
    ~ble_advertiser();

    ble_advertiser(const ble_advertiser&)            = delete;
    ble_advertiser& operator=(const ble_advertiser&) = delete;

    void register_callbacks(const BLEAdvertiserCallbacks& callbacks);
    [[nodiscard]] int32_t get_status() const;
    void start(const BLEAdvertiseConfig& config);
    void stop();

    void add_service(const char* service_uuid);
    void add_characteristic(ble_characteristics characteristics) const;
    void add_descriptor(const char* characteristic_uuid, const char* descriptor_uuid) const;

    bool send_notification(const char* device_address, const char* characteristic_uuid, const uint8_t* value,
                           size_t value_len) const;

    static void respond_read(std::unique_ptr<BLERequestContext> request, const uint8_t* data, size_t len,
                             int32_t status);
    static void respond_write(std::unique_ptr<BLERequestContext> request, int32_t status);

private:
    GDBusConnection* m_dbus_connection = nullptr;
    Adapter* m_adapter                 = nullptr;
    Advertisement* m_advertisement     = nullptr;
    Application* m_app                 = nullptr;
    Agent* m_agent                     = nullptr;

    std::string m_primary_service_uuid;
    BLEAdvertiserCallbacks m_callbacks{};
    mutable std::recursive_mutex m_mutex;
    bool m_is_advertising = false;

    static const char* on_char_read_cb(const Application* app, const char* address, const char* service_uuid,
                                       const char* char_uuid, guint16 mtu, guint16 offset);
    static const char* on_char_write_cb(const Application* app, const char* address, const char* service_uuid,
                                        const char* char_uuid, GByteArray* byteArray, guint16 mtu, guint16 offset);

    static const char* on_desc_read_cb(const Application* application, const char* address, const char* service_uuid,
                                       const char* char_uuid, const char* desc_uuid);
    static const char* on_desc_write_cb(const Application* application, const char* address, const char* service_uuid,
                                        const char* char_uuid, const char* desc_uuid, const GByteArray* byteArray);
    static void on_char_updated(const Application* application, const char* service_uuid, const char* char_uuid,
                                GByteArray* byteArray);
};

#endif
