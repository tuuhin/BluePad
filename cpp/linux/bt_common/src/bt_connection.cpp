#include "bt_connection.h"
#include "bt_common_utils.h"

#include <gio/gio.h>

bt_connection& bt_connection::instance() {
    static bt_connection instance;
    return instance;
}

bt_connection::bt_connection() {
    auto* dbus_conn = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, nullptr);
    if (dbus_conn == nullptr) throw std::runtime_error("Failed to connect to the system D-Bus.");

    m_adapter = binc_adapter_get_default(dbus_conn);
    g_object_unref(dbus_conn);

    if (m_adapter == nullptr) throw std::runtime_error("No default Bluetooth adapter found.");
    LINUX_LOG("BLUETOOTH ADAPTER INIT SUCCESSFULLY.");
}

bt_connection::~bt_connection() { m_onStatusChange = nullptr; }

std::future<bool> bt_connection::is_ble_secure_connection_available() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        const char* address = binc_adapter_get_address(m_adapter);

        return address != nullptr && std::string(address) != "00:00:00:00:00:00";
    });
}

std::future<bool> bt_connection::is_peripheral_role_supported() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        return utils::check_adapter_role_supported(m_adapter, "peripheral") == TRUE;
    });
}

std::future<bool> bt_connection::is_bluetooth_active() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        return binc_adapter_get_powered_state(m_adapter) == 1;
    });
}

void bt_connection::on_adapter_powered_changed(Adapter*, gboolean is_powered) {

    auto& instance = bt_connection::instance();

    std::lock_guard lock(instance.m_mutex);

    if (instance.m_onStatusChange) instance.m_onStatusChange(static_cast<bool>(is_powered));
}

std::future<void> bt_connection::register_bt_listener(const std::function<void(bool)>& callback) {

    return std::async(std::launch::async, [this, callback]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return;

        m_onStatusChange = callback;

        binc_adapter_set_powered_state_cb(m_adapter, &bt_connection::on_adapter_powered_changed);
    });
}

void bt_connection::unregister_bt_listener() {
    std::lock_guard lock(m_mutex);

    if (m_adapter == nullptr) return;

    binc_adapter_set_powered_state_cb(m_adapter, nullptr);
    m_onStatusChange = nullptr;
}
