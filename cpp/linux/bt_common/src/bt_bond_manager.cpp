#include <stdexcept>
#include <thread>

#include <binc/adapter.h>
#include <binc/agent.h>
#include <binc/device.h>
#include <glib-object.h>

#include "bt_bond_manager.h"

#include "bt_common_utils.h"
#include "bt_enums.h"

auto BOND_MANAGER_KEY = "bt_bond_manager_instance";

bluetooth_bond_manager& bluetooth_bond_manager::instance() {
    static bluetooth_bond_manager instance;
    return instance;
}

bluetooth_bond_manager::bluetooth_bond_manager() {
    auto* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, nullptr);
    if (dbusConnection == nullptr) throw std::runtime_error("UNABLE TO CONNECT TO THE SYSTEM BUS");

    m_adapter = binc_adapter_get_default(dbusConnection);
    g_object_unref(dbusConnection);

    if (m_adapter == nullptr) throw std::runtime_error("UNABLE TO READ THE BLUETOOTH ADAPTER");
    LINUX_LOG("CREATED INSTANCE SUCCESSFULLY");
}

bluetooth_bond_manager::~bluetooth_bond_manager() { unregister_bond_callback(); }

std::future<bt_bond_state> bluetooth_bond_manager::get_bond_state(const std::string& device_address) const {
    return std::async(std::launch::async, [device_address, this]() -> bt_bond_state {
        if (m_adapter == nullptr) return ERROR_UNKNOWN;

        const auto* device = binc_adapter_get_device_by_address(m_adapter, device_address.c_str());
        if (device == nullptr) return ERROR_INVALID_DEVICE;

        auto const is_paired = binc_device_get_paired(device);
        return is_paired ? DEVICE_BONDED : DEVICE_NOT_BONDED;
    });
}

void bluetooth_bond_manager::handle_display_passkey_event(const std::string& device_path, guint32 passkey) {
    std::lock_guard lock(m_mutex);
    if (!m_pairing_callback.on_confirm_pin) return;

    m_pairing_callback.on_confirm_pin(std::to_string(passkey).c_str(), nullptr);
}

bool bluetooth_bond_manager::handle_request_confirmation_event(const std::string& device_path, guint32 passkey, GDBusMethodInvocation* invocation) {
    std::lock_guard lock(m_mutex);
    if (!m_pairing_callback.on_confirm_pin) return false;

    auto responder = std::make_shared<bluetooth_bond_callback_responder>(device_path, nullptr, "RequestConfirmation");
    responder->dbus_invocation = invocation;
    m_responders.insert(responder);

    const auto raw_handle = reinterpret_cast<void*>(&responder);
    m_pairing_callback.on_confirm_pin(std::to_string(passkey).c_str(), raw_handle);
    return true;
}

std::future<void> bluetooth_bond_manager::request_and_register_bond_callback(const std::string& device_address,
                                                                             bt_bond_pairing_callback callback) {
    return std::async(std::launch::async, [this, callback, device_address]() {
        std::lock_guard lock(m_mutex);
        m_pairing_callback = callback;

        if (bond_m_agent == nullptr) {
            LINUX_LOG("CREATED A NEW BOND AGENT");

            m_custom_agent_id = register_custom_agent(dbusConnection, "/org/bluez/CustomBincAgent", this);
          g_object_unref(dbusConnection);
        }

        auto* device = binc_adapter_get_device_by_address(m_adapter, device_address.c_str());
        if (device == nullptr && m_pairing_callback.on_error != nullptr) {

            LINUX_LOG("UNABLE TO READ THE DEVICE FROM ADDRESS");
            m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_DEVICE_CANNOT_BE_FOUND);
            return;
        }
        binc_device_pair(device);
    });
}

void bluetooth_bond_manager::unregister_bond_callback() {
    std::lock_guard lock(m_mutex);

    if (bond_m_agent != nullptr) {
        LINUX_LOG("CLEARING BOND AGENT");
        binc_agent_free(bond_m_agent);
        bond_m_agent = nullptr;
    }

    m_responders.clear();
    m_pairing_callback = {nullptr, nullptr, nullptr};
}

void bluetooth_bond_manager::accept_connection(const std::string& pin,
                                               const bt_bond_responder_handle& callback_responder) {
    std::lock_guard lock(m_mutex);
    if (callback_responder == nullptr) return;

    const auto* shared_ptr_addr = static_cast<std::shared_ptr<bluetooth_bond_callback_responder>*>(callback_responder);

    const auto responder = *shared_ptr_addr;

    if (responder && responder->device_raw && m_pairing_callback.on_results) {
        m_pairing_callback.on_results(RESPONSE_PAIRED);
    }
    m_responders.erase(responder);
}

void bluetooth_bond_manager::cancel_connection(const bt_bond_responder_handle& callback_responder) {
    std::lock_guard lock(m_mutex);

    if (callback_responder == nullptr) return;

    const auto* shared_ptr_addr = static_cast<std::shared_ptr<bluetooth_bond_callback_responder>*>(callback_responder);

    const auto responder = *shared_ptr_addr;

    if (responder && m_pairing_callback.on_error) {
        m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_OPERATION_CANCELLED);
    }
    m_responders.erase(responder);
}
