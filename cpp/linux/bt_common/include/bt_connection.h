#ifndef LINUX_BT_CALLS_H
#define LINUX_BT_CALLS_H

#include <binc/adapter.h>

#include <functional>
#include <future>
#include <mutex>

class bt_connection {
public:
    static bt_connection& instance();

    bt_connection(const bt_connection&)            = delete;
    bt_connection& operator=(const bt_connection&) = delete;

    std::future<bool> is_ble_secure_connection_available();
    std::future<bool> is_peripheral_role_supported();
    std::future<bool> is_bluetooth_active();

    std::future<void> register_bt_listener(const std::function<void(bool)>& callback);
    void unregister_bt_listener();

private:
    bt_connection();
    ~bt_connection();

    static void on_adapter_powered_changed(Adapter* adapter, gboolean is_powered);

    std::mutex m_mutex;
    Adapter* m_adapter = nullptr;

    std::function<void(bool)> m_onStatusChange;
};

#endif