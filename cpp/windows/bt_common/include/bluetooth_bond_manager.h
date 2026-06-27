#ifndef WINDOWS_BLE_BLUETOOTH_BOND_MANAGER_H
#define WINDOWS_BLE_BLUETOOTH_BOND_MANAGER_H

#include "bluetooth_enums.h"
#include "bt_common_c_api.h"

#include <memory>
#include <mutex>
#include <set>
#include <utility>
#include <winrt/Windows.Devices.Enumeration.h>
#include <winrt/Windows.Devices.Radios.h>
#include <winrt/base.h>

using namespace winrt::Windows::Foundation;
using namespace winrt::Windows::Devices::Radios;
using namespace winrt::Windows::Devices::Enumeration;

struct bluetooth_bond_callback_responder {
    DevicePairingRequestedEventArgs event_args;

    explicit bluetooth_bond_callback_responder(DevicePairingRequestedEventArgs args) : event_args(std::move(args)) {}
};

class bluetooth_bond_manager : public std::enable_shared_from_this<bluetooth_bond_manager> {

    std::mutex m_mutex;
    winrt::event_token m_pairing_token;
    DeviceInformationCustomPairing m_custom_pairing{nullptr};
    std::set<bluetooth_bond_callback_responder*> m_responders;

public:
    static IAsyncOperation<int8_t> get_bond_state(const std::string& device_address);

    IAsyncAction request_and_register_bond_callback(const std::string& device_address,
                                                    bt_bond_pairing_callback callback);

    void unregister_bond_callback();

    void accept_connection(const bt_bond_responder_handle& callback_responder);
    void cancel_connection(const bt_bond_responder_handle& callback_responder);
};

#endif // WINDOWS_BLE_BLUETOOTH_BOND_MANAGER_H
