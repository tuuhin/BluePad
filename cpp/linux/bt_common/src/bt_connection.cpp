#include "bt_connection.h"
#include "bt_common_utils.h"

#include <gio/gio.h>

bt_connection& bt_connection::instance() {
    static bt_connection instance;
    return instance;
}

bt_connection::bt_connection() {
    auto* dbus_conn = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, nullptr);
    if (dbus_conn == nullptr) throw std::runtime_error("UNABLE TO CONNECT TO THE SYSTEM BUS");

    m_adapter = binc_adapter_get_default(dbus_conn);
    g_object_unref(dbus_conn);

    if (m_adapter == nullptr) throw std::runtime_error("NO DEFAULT BLUETOOTH ADAPTER PRESENT");
    LINUX_LOG("CREATED INSTANCE SUCCESSFULLY");
}

bt_connection::~bt_connection() {
    unregister_bt_listener();
    LINUX_LOG("DESTROYING INSTANCE");
}

std::future<bool> bt_connection::is_ble_secure_connection_available() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        const char* address = binc_adapter_get_address(m_adapter);

        return address != nullptr && strcmp(address, "00:00:00:00:00:00") == 0;
    });
}

std::future<bool> bt_connection::is_peripheral_role_supported() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        LINUX_LOG("CHECKING ADAPTER ROLE");
        return utils::check_adapter_role_supported(m_adapter, "peripheral") == TRUE;
    });
}

std::future<bool> bt_connection::is_bluetooth_active() {
    return std::async(std::launch::async, [this]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return false;

        LINUX_LOG("BLUETOOTH ADAPTER POWER STATE");
        return binc_adapter_get_powered_state(m_adapter) == 1;
    });
}

void bt_connection::on_adapter_powered_changed(Adapter*, const gboolean is_powered) {
    auto& instance = bt_connection::instance();

    std::lock_guard lock(instance.m_mutex);

    if (instance.m_onStatusChange == nullptr) return;
    instance.m_onStatusChange(static_cast<bool>(is_powered));
}

std::future<void> bt_connection::register_bt_listener(const std::function<void(bool)>& callback) {
    return std::async(std::launch::async, [this, callback]() {
        std::lock_guard lock(m_mutex);

        if (m_adapter == nullptr) return;

        m_onStatusChange = callback;
        if (m_isListenerRegistered) return;

        LINUX_LOG("REGISTERING LISTENER FOR BT POWER STATE");
        binc_adapter_set_powered_state_cb(m_adapter, &bt_connection::on_adapter_powered_changed);
        m_isListenerRegistered = true;
    });
}

void bt_connection::unregister_bt_listener() {
    std::lock_guard lock(m_mutex);
    LINUX_LOG("CLEARING VARIABLES");
    if (m_adapter == nullptr) return;
    m_onStatusChange       = nullptr;
    m_isListenerRegistered = false;
}
