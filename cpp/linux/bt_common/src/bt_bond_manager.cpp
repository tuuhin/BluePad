#include <stdexcept>
#include <thread>

#include <binc/adapter.h>
#include <binc/agent.h>
#include <binc/device.h>
#include <glib-object.h>

#include "bt_bond_manager.h"
#include "bt_enums.h"

auto BOND_MANAGER_KEY = "bt_bond_manager_instance";

bluetooth_bond_manager::bluetooth_bond_manager() {
    auto* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, nullptr);
    m_adapter            = binc_adapter_get_default(dbusConnection);
    if (m_adapter == nullptr)
        throw std::runtime_error("Failed to acquire default Bluetooth adapter for pairing operations");
    g_object_set_data(G_OBJECT(m_adapter), BOND_MANAGER_KEY, this);
}

bluetooth_bond_manager::~bluetooth_bond_manager() {
    unregister_bond_callback();
    if (m_adapter == nullptr) return;
    g_object_set_data(G_OBJECT(m_adapter), BOND_MANAGER_KEY, nullptr);
}

std::future<int8_t> bluetooth_bond_manager::get_bond_state(const std::string& device_address) {
    return std::async(std::launch::async, [device_address]() -> int8_t {
        auto* dbusConnection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, nullptr);
        const auto* adapter  = binc_adapter_get_default(dbusConnection);
        if (adapter == nullptr) return 0;

        const auto* device = binc_adapter_get_device_by_address(adapter, device_address.c_str());
        if (device == nullptr) return 0;
        auto const is_paired = binc_device_get_paired(device);
        return is_paired ? 1 : 0;
    });
}

gboolean bluetooth_bond_manager::on_request_authorization(Device* device) {
    if (device == nullptr) return FALSE;

    Adapter* adapter     = binc_device_get_adapter(device);
    const auto* instance = static_cast<bluetooth_bond_manager*>(g_object_get_data(G_OBJECT(adapter), BOND_MANAGER_KEY));

    if (instance == nullptr) return FALSE;
    if (instance->m_pairing_callback.on_confirm_pin) return FALSE;
    std::string address = binc_device_get_address(device);

    auto responder = std::make_shared<bluetooth_bond_callback_responder>(address, device, "RequestAuthorization");
    instance->m_responders.insert(responder);

    const auto raw_handle = reinterpret_cast<void*>(&responder);
    instance->m_pairing_callback.on_confirm_pin("", raw_handle);

    return TRUE;
}

guint32 bluetooth_bond_manager::on_request_passkey(Device* device) {
    if (device == nullptr) return 0;
    Adapter* adapter     = binc_device_get_adapter(device);
    const auto* instance = static_cast<bluetooth_bond_manager*>(g_object_get_data(G_OBJECT(adapter), BOND_MANAGER_KEY));

    if (instance == nullptr) return 0;
    if (instance->m_pairing_callback.on_confirm_pin) {
        std::string address = binc_device_get_address(device);

        auto responder = std::make_shared<bluetooth_bond_callback_responder>(address, device, "RequestPasskey");
        instance->m_responders.insert(responder);

        const auto raw_handle = reinterpret_cast<void*>(&responder);
        instance->m_pairing_callback.on_confirm_pin("", raw_handle);
    }
    return 0;
}

std::future<void> bluetooth_bond_manager::request_and_register_bond_callback(const std::string& device_address,
                                                                             bt_bond_pairing_callback callback) {
    return std::async(std::launch::async, [this, callback, device_address]() {
        std::lock_guard lock(m_mutex);
        m_pairing_callback = callback;

        if (m_agent == nullptr) {
            m_agent = binc_agent_create(m_adapter, "/org/bluez/BincAgent", KEYBOARD_DISPLAY);
            binc_agent_set_request_authorization_cb(m_agent, &bluetooth_bond_manager::on_request_authorization);
            binc_agent_set_request_passkey_cb(m_agent, &bluetooth_bond_manager::on_request_passkey);
        }

        // 2. Locate the specific target device we want to pair with
        auto* device = binc_adapter_get_device_by_address(m_adapter, device_address.c_str());
        if (device == nullptr && m_pairing_callback.on_error != nullptr) {
            m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_DEVICE_CANNOT_BE_FOUND);
            return;
        }
        binc_device_pair(device);
    });
}

void bluetooth_bond_manager::unregister_bond_callback() {
    std::lock_guard lock(m_mutex);
    if (m_agent != nullptr) {
        binc_agent_free(m_agent);
        m_agent = nullptr;
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
    if (!callback_responder) return;

    const auto* shared_ptr_addr = static_cast<std::shared_ptr<bluetooth_bond_callback_responder>*>(callback_responder);

    const auto responder = *shared_ptr_addr;

    if (responder && m_pairing_callback.on_error) {
        m_pairing_callback.on_error(BT_BOND_REQUEST_ERROR_OPERATION_CANCELLED);
    }
    m_responders.erase(responder);
}