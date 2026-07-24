#include <algorithm>
#include <binc/adapter.h>
#include <binc/device.h>
#include <cstring>
#include <gio/gio.h>
#include <glib-object.h>
#include <iostream>
#include <stdexcept>
#include <thread>

#include "bt_bond_manager.h"
#include "bt_common_utils.h"
#include "bt_enums.h"
#include "custom_bluez.h"

bluetooth_bond_manager::bluetooth_bond_manager() {
    GError* error        = nullptr;
    auto* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
    if (dbusConnection == nullptr) {
        const std::string errormessage = "UNABLE TO CONNECT TO THE SYSTEM BUS";
        if (error != nullptr) throw std::runtime_error(errormessage + error->message);
        throw std::runtime_error(errormessage);
    }

    m_adapter = binc_adapter_get_default(dbusConnection);
    g_object_unref(dbusConnection);

    if (m_adapter == nullptr) throw std::runtime_error("UNABLE TO READ THE BLUETOOTH ADAPTER");
    LINUX_LOG("CREATED INSTANCE SUCCESSFULLY");
}

bluetooth_bond_manager::~bluetooth_bond_manager() { unregister_bond_callback(); }

bt_bond_state bluetooth_bond_manager::get_bond_state(const std::string& device_address) {
    GError* error                   = nullptr;
    GDBusConnection* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
    if (dbusConnection == nullptr) {
        if (error != nullptr) g_error_free(error);
        return ERROR_UNKNOWN;
    }

    Adapter* adapter = binc_adapter_get_default(dbusConnection);
    g_object_unref(dbusConnection);

    if (adapter == nullptr) return ERROR_UNKNOWN;

    const auto* device = binc_adapter_get_device_by_address(adapter, device_address.c_str());

    if (device == nullptr) {
        LINUX_LOG("SORRY CANNOT FIND THE DEVICE");
        return ERROR_INVALID_DEVICE;
    }

    LINUX_LOG("READING DEVICE STATE");
    const bool is_paired = binc_device_get_paired(device);

    return is_paired ? DEVICE_BONDED : DEVICE_NOT_BONDED;
}

void bluetooth_bond_manager::handle_display_passkey_event(const std::string&, const guint32 passkey) const {
    std::lock_guard lock(m_mutex);
    if (!m_pairing_callback.on_confirm_pin) {
        LINUX_LOG("CANNOT PROCESS CONFIRM PIN");
        return;
    }
    char pin_str[7];
    snprintf(pin_str, sizeof(pin_str), "%06u", passkey);
    m_pairing_callback.on_confirm_pin(pin_str, nullptr);
}

bool bluetooth_bond_manager::handle_request_confirmation_event(const std::string& device_path, const guint32 passkey,
                                                               GDBusMethodInvocation* invocation) const {
    std::lock_guard lock(m_mutex);
    if (!m_pairing_callback.on_confirm_pin) {
        LINUX_LOG("CANNOT PROCESS CONFIRM PIN");
        return false;
    }

    const auto responder =
        std::make_shared<bluetooth_bond_callback_responder>(device_path, nullptr, "RequestConfirmation");
    responder->dbus_invocation = invocation;
    m_responders.insert(responder);

    char pin_str[7];
    snprintf(pin_str, sizeof(pin_str), "%06u", passkey);
    const auto raw_handle = static_cast<void*>(responder.get());
    m_pairing_callback.on_confirm_pin(pin_str, raw_handle);
    return true;
}

void bluetooth_bond_manager::handle_cancel_event() const {
    std::lock_guard lock(m_mutex);

    if (m_pairing_callback.on_error) {
        m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_OPERATION_CANCELLED);
    }

    for (const auto& responder : m_responders) {
        if (responder == nullptr || responder->dbus_invocation == nullptr) continue;
        g_dbus_method_invocation_return_dbus_error(responder->dbus_invocation, "org.bluez.Error.Canceled",
                                                   "Pairing was canceled by remote device");
    }
    m_responders.clear();
}

std::future<void> bluetooth_bond_manager::request_and_register_bond_callback(const std::string& device_address,
                                                                             bt_bond_pairing_callback callback) {
    return std::async(std::launch::async, [this, callback, device_address]() {
        std::lock_guard lock(m_mutex);
        m_pairing_callback = callback;

        if (m_custom_agent_id == 0) {
            LINUX_LOG("REGISTERING CUSTOM BOND AGENT");

            if (GDBusConnection* dbus_conn = binc_adapter_get_dbus_connection(m_adapter); dbus_conn != nullptr) {
                m_custom_agent_id = register_custom_agent(dbus_conn, "/org/bluez/CustomBincAgent", this);
            } else {
                LINUX_LOG("FAILED TO GET DBUS CONNECTION FROM ADAPTER");
                if (m_pairing_callback.on_error) {
                    m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_DEVICE_CANNOT_BE_FOUND);
                }
                return;
            }
        }

        auto* device = binc_adapter_get_device_by_address(m_adapter, device_address.c_str());
        if (device == nullptr) {
            LINUX_LOG("UNABLE TO READ THE DEVICE FROM ADDRESS");
            if (m_pairing_callback.on_error) {
                m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_DEVICE_CANNOT_BE_FOUND);
            }
            return;
        }

        binc_device_pair(device);
    });
}

void bluetooth_bond_manager::unregister_bond_callback() {
    std::lock_guard lock(m_mutex);

    if (m_custom_agent_id > 0) {
        LINUX_LOG("UNREGISTERING BOND AGENT");
        if (GDBusConnection* dbus_conn = binc_adapter_get_dbus_connection(m_adapter); dbus_conn != nullptr) {
            g_dbus_connection_unregister_object(dbus_conn, m_custom_agent_id);
        }
        m_custom_agent_id = 0;
    }

    m_responders.clear();
    m_pairing_callback = {.on_results = nullptr, .on_confirm_pin = nullptr, .on_error = nullptr};
}

void bluetooth_bond_manager::accept_connection(const std::string& pin,
                                               const bt_bond_responder_handle& callback_responder) const {
    std::lock_guard lock(m_mutex);
    if (callback_responder == nullptr) return;

    const auto* raw_responder = static_cast<bluetooth_bond_callback_responder*>(callback_responder);

    const auto it = std::ranges::find_if(
        m_responders, [raw_responder](const std::shared_ptr<bluetooth_bond_callback_responder>& ptr) {
            return ptr.get() == raw_responder;
        });

    if (it == m_responders.end()) return;
    // Reply SUCCESS to BlueZ over D-Bus
    if ((*it)->dbus_invocation) {
        g_dbus_method_invocation_return_value((*it)->dbus_invocation, nullptr);
    }

    if (m_pairing_callback.on_results) {
        m_pairing_callback.on_results(RESPONSE_PAIRED);
    }
    m_responders.erase(it);
}

void bluetooth_bond_manager::cancel_connection(const bt_bond_responder_handle& callback_responder) const {
    std::lock_guard lock(m_mutex);
    if (callback_responder == nullptr) return;

    const auto* raw_responder = static_cast<bluetooth_bond_callback_responder*>(callback_responder);

    const auto it = std::ranges::find_if(
        m_responders, [raw_responder](const std::shared_ptr<bluetooth_bond_callback_responder>& ptr) {
            return ptr.get() == raw_responder;
        });

    if (it == m_responders.end()) return;

    // 💡 Reply REJECTED error to BlueZ over D-Bus
    if ((*it)->dbus_invocation) {
        g_dbus_method_invocation_return_dbus_error((*it)->dbus_invocation, "org.bluez.Error.Rejected",
                                                   "User rejected the numeric pairing confirmation");
    }

    if (m_pairing_callback.on_error) {
        m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_OPERATION_CANCELLED);
    }

    m_responders.erase(it);
}