#ifndef LINUX_BT_BOND_MANAGER_H
#define LINUX_BT_BOND_MANAGER_H

#include <binc/adapter.h>
#include <future>
#include <gio/gio.h>
#include <glib-object.h>
#include <memory>
#include <mutex>
#include <set>
#include <string>
#include <utility>

#include "bt_common_defination.h"
#include "bt_enums.h"

struct bluetooth_bond_callback_responder {
    std::string device_address;
    Device* device_raw = nullptr;
    std::string request_type;
    GDBusMethodInvocation* dbus_invocation = nullptr;

    bluetooth_bond_callback_responder(std::string addr, Device* device, std::string type,
                                      GDBusMethodInvocation* invocation = nullptr)
        : device_address(std::move(addr)), device_raw(device), request_type(std::move(type)),
          dbus_invocation(invocation) {}
};

class bluetooth_bond_manager : public std::enable_shared_from_this<bluetooth_bond_manager> {
public:
    bluetooth_bond_manager();
    ~bluetooth_bond_manager();

    static bt_bond_state get_bond_state(const std::string& device_address) ;

    std::future<void> request_and_register_bond_callback(const std::string& device_address,
                                                         bt_bond_pairing_callback callback);

    void unregister_bond_callback();

    // UI Response handlers
    void accept_connection(const std::string& pin, const bt_bond_responder_handle& callback_responder) const;
    void cancel_connection(const bt_bond_responder_handle& callback_responder) const;

    // Internal event triggers from D-Bus handle_method_call
    void handle_display_passkey_event(const std::string& device_path, guint32 passkey) const;
    bool handle_request_confirmation_event(const std::string& device_path, guint32 passkey,
                                           GDBusMethodInvocation* invocation) const;
    void handle_cancel_event() const;

private:
    mutable std::mutex m_mutex;
    Adapter* m_adapter      = nullptr;
    Agent* bond_m_agent     = nullptr;
    guint m_custom_agent_id = 0;

    bt_bond_pairing_callback m_pairing_callback{.on_results = nullptr, .on_confirm_pin = nullptr, .on_error = nullptr};
    mutable std::set<std::shared_ptr<bluetooth_bond_callback_responder>> m_responders;

    GDBusMethodInvocation* m_active_invocation = nullptr;
    std::string m_active_device_path;
};

#endif // LINUX_BT_BOND_MANAGER_H