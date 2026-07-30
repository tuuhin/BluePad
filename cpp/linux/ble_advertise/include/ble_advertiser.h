#ifndef LINUX_BLE_BLE_ADVERTISE_H
#define LINUX_BLE_BLE_ADVERTISE_H

#include <application.h>
#include <glib.h>
#include <memory>
#include <mutex>
#include <string>
#include <variant>
#include <vector>

#include "ble_advertise_c.h"

#include <thread>

struct BLERequestContext {

    struct ReadCharacteristics {
        std::string service_uuid;
        std::string char_uuid;
    };

    struct WriteCharacteristics {
        std::string service_uuid;
        std::string char_uuid;
        std::vector<uint8_t> value;
    };

    std::variant<ReadCharacteristics, WriteCharacteristics> data;

    const Application* app_ref = nullptr;
    bool completed             = false;

    BLERequestContext(const Application* app, std::string service_uuid, std::string char_uuid)
        : data(ReadCharacteristics{.service_uuid = std::move(service_uuid), .char_uuid = std::move(char_uuid)}),
          app_ref(app) {}

    BLERequestContext(const Application* app, std::string service_uuid, std::string char_uuid, std::vector<uint8_t> val)
        : data(WriteCharacteristics{
              .service_uuid = std::move(service_uuid), .char_uuid = std::move(char_uuid), .value = std::move(val)}),
          app_ref(app) {}

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

    // need a threaded main loop for the advertiser to run
    GMainLoop* m_main_loop = nullptr;
    std::thread m_glib_thread;

    // flags
    bool m_is_secondary_channel_supported = false;
    uint8_t max_advertising_length         = 31;

    static ble_advertiser* s_instance;
    static std::mutex s_instance_mutex;
};

#endif
