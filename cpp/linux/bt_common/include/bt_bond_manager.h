#ifndef LINUX_BT_BOND_MANAGER_H
#define LINUX_BT_BOND_MANAGER_H

#include "bt_common_c.h"
#include "bt_common_defination.h"
#include "bt_enums.h"

#include <binc/adapter.h>
#include <future>
#include <gio/gio.h>
#include <glib-object.h>
#include <memory>
#include <mutex>
#include <set>
#include <utility>

struct bluetooth_bond_callback_responder {
    std::string device_address;
    Device* device_raw;
    std::string request_type;

    bluetooth_bond_callback_responder(std::string addr, Device* device, std::string type)
        : device_address(std::move(addr)), device_raw(device), request_type(std::move(type)) {}

    bool operator<(const bluetooth_bond_callback_responder& other) const {
        return device_address < other.device_address;
    }
};

class bluetooth_bond_manager : public std::enable_shared_from_this<bluetooth_bond_manager> {

    std::mutex m_mutex{};
    Adapter* m_adapter;
    Agent* m_agent = nullptr;
    bt_bond_pairing_callback m_pairing_callback{nullptr, nullptr, nullptr};
    mutable std::set<std::shared_ptr<bluetooth_bond_callback_responder>> m_responders;

    static gboolean on_request_authorization(Device* device);
    static guint32 on_request_passkey(Device* device);

public:
    bluetooth_bond_manager();
    ~bluetooth_bond_manager();

    static std::future<int8_t> get_bond_state(const std::string& device_address);

    std::future<void> request_and_register_bond_callback(const std::string& device_address,
                                                         bt_bond_pairing_callback callback);

    void unregister_bond_callback();

    void accept_connection(const std::string& pin, const bt_bond_responder_handle& callback_responder);
    void cancel_connection(const bt_bond_responder_handle& callback_responder);
};

#endif