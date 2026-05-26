#ifndef WINDOWS_BLE_BT_CALLS_H
#define WINDOWS_BLE_BT_CALLS_H

#include <functional>
#include <mutex>
#include <winrt/Windows.Devices.Radios.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/base.h>

using namespace winrt::Windows::Foundation;
using namespace winrt::Windows::Devices::Radios;

class bluetooth_caller {

    std::mutex m_mutex;
    Radio m_selected_bt_radio{nullptr};

    winrt::event_token m_eventToken;
    std::function<void(bool)> m_onStatusChange;

public:
    static IAsyncOperation<bool> is_ble_secure_connection_available();
    static IAsyncOperation<bool> is_peripheral_role_supported();

    static IAsyncOperation<bool> is_device_paired(const std::string& device_address);
    static IAsyncOperation<bool> try_pairing_device(const std::string& device_address,
                                                    const std::function<void(bool)>& callback);

    IAsyncOperation<bool> is_bluetooth_active();
    IAsyncAction register_bt_listener(const std::function<void(bool)>& callback);
    void unregister_bt_listener();
};

#endif
