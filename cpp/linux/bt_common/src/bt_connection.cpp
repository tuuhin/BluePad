#include "bt_connection.h"
#include "bt_common_utils.h"
#include "device_internal.h"

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
bt_request_enable_status bt_connection::request_bt_enable() {
    GError* error         = nullptr;
    GDBusConnection* conn = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
    if (conn == nullptr) {
        if (error != nullptr) g_error_free(error);
        LINUX_LOG("ACCESS DENIED CANNOT ENABLE BLUETOOTH FROM HERE");
        return REQUEST_DENIED_PRIVACY_ISSUES;
    }

    // binc setup
    Adapter* adapter = binc_adapter_get_default(conn);
    if (adapter != nullptr) {
        if (binc_adapter_get_powered_state(adapter) != FALSE) {
            LINUX_LOG("BLUETOOTH ALREADY ENABLED NO NEED TO ENABLE IT AGAIN");
            g_object_unref(conn);
            return REQUEST_NOT_NEEDED;
        }
        LINUX_LOG("ENABLING BIN ADAPTER");
        binc_adapter_power_on(adapter);

        if (binc_adapter_get_powered_state(adapter) != FALSE) {
            LINUX_LOG("REQUEST ACCEPTED BY THE USER");
            g_object_unref(conn);
            return REQUEST_ACCEPTED;
        }
        return REQUEST_DENIED_UNKNOWN;
    }

    std::string target_path;
    LINUX_LOG("CANNOT FOUND ADAPTER LOOKING WITH D-BUS ");

    GVariant* managed_objs = g_dbus_connection_call_sync(
        conn, "org.bluez", "/", "org.freedesktop.DBus.ObjectManager", "GetManagedObjects", nullptr,
        G_VARIANT_TYPE("(a{oa{sa{sv}}})"), G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (managed_objs != nullptr) {
        GVariantIter* objects_iter = nullptr;
        g_variant_get(managed_objs, "(a{oa{sa{sv}}})", &objects_iter);

        const char* object_path     = nullptr;
        GVariant* i_faces_and_props = nullptr;

        while (g_variant_iter_loop(objects_iter, "{&o@a{sa{sv}}}", &object_path, &i_faces_and_props)) {
            if (g_variant_lookup_value(i_faces_and_props, "org.bluez.Adapter1", nullptr) == nullptr) continue;
            target_path = object_path;
        }
        g_variant_iter_free(objects_iter);
        g_variant_unref(managed_objs);
    }

    if (error != nullptr) g_error_free(error);
    if (target_path.empty()) {
        LINUX_LOG("UNABLE TO FIND ANY BLUETOOTH ADAPTER");
        g_object_unref(conn);
        return REQUEST_DENIED_CANNOT_FIND_ADAPTER;
    }

    LINUX_LOG("FOUND ADAPTER AT " + target_path);

    GVariant* set_result = g_dbus_connection_call_sync(
        conn, "org.bluez", target_path.c_str(), "org.freedesktop.DBus.Properties", "Set",
        g_variant_new("(ssv)", "org.bluez.Adapter1", "Powered", g_variant_new_variant(g_variant_new_boolean(TRUE))),
        nullptr, G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (set_result == nullptr) {
        if (error != nullptr) {
            LINUX_LOG("CANNOT SET RADIO STATE EXCEPTION: " << error->message);
            if (error->code == G_DBUS_ERROR_ACCESS_DENIED || error->code == G_DBUS_ERROR_AUTH_FAILED) {
                LINUX_LOG("ACCESS DENIED BY SYSTEM");
                g_error_free(error);
                g_object_unref(conn);
                return REQUEST_DENIED_BY_SYSTEM;
            }
            g_error_free(error);
        }
        g_object_unref(conn);
        return REQUEST_DENIED_UNKNOWN;
    }

    g_variant_unref(set_result);
    g_object_unref(conn);

    LINUX_LOG("REQUEST ACCEPTED BY THE USER");
    return REQUEST_ACCEPTED;
}
