#include "bluetooth_caller.h"
#include "bluetooth_enums.h"
#include <iostream>
#include <mutex>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Enumeration.h>
#include <winrt/Windows.Foundation.Collections.h>

using namespace std;
using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Radios;
using namespace Windows::Devices::Enumeration;

#define WIN_LOG(msg) wclog << "[NATIVE-WINDOWS] " << msg << endl;

IAsyncOperation<bool> bluetooth_caller::is_ble_secure_connection_available() {
    init_apartment();
    const auto adapter = co_await BluetoothAdapter::GetDefaultAsync();
    co_return adapter.AreLowEnergySecureConnectionsSupported();
}

IAsyncOperation<bool> bluetooth_caller::is_peripheral_role_supported() {
    init_apartment();
    const auto adapter = co_await BluetoothAdapter::GetDefaultAsync();
    co_return adapter.IsPeripheralRoleSupported();
}

IAsyncOperation<int8_t> bluetooth_caller::is_device_bonded(const std::string& device_address) {
    init_apartment();

    try {
        const auto bt_address = static_cast<unsigned int>(std::stoul(device_address));
        const auto device     = co_await BluetoothLEDevice::FromBluetoothAddressAsync(bt_address);

        if (device == nullptr) {
            WIN_LOG(L"UNABLE TO READ DEVICE FROM THE GIVEN DEVICE ID " << to_hstring(device_address));
            co_return ERROR_INVALID_DEVICE;
        }

        const auto device_info = co_await DeviceInformation::CreateFromIdAsync(device.DeviceId());
        const auto pairState   = device_info.Pairing();
        co_return pairState.IsPaired() ? DEVICE_BONDED : DEVICE_NOT_BONDED;
    } catch (const hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WINRT EXCEPTION WHILE CHECKING BOND STATE HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                      << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION OCCURRED");
    }
    co_return ERROR_UNKNOWN;
}

IAsyncOperation<int> bluetooth_caller::request_device_bond(const std::string& device_address,
                                                           uint32_t timeout_in_millis) {

    const auto bt_address = static_cast<unsigned int>(std::stoul(device_address));
    const auto device     = co_await BluetoothLEDevice::FromBluetoothAddressAsync(bt_address);

    if (device == nullptr) {
        WIN_LOG(L"UNABLE TO READ DEVICE FROM THE GIVEN DEVICE ID " << to_hstring(device_address));
        co_return - 1;
    }

    const auto device_info = co_await DeviceInformation::CreateFromIdAsync(device.DeviceId());
    const auto pair_state  = device_info.Pairing();

    if (pair_state.IsPaired()) {
        WIN_LOG(L"DEVICE WITH ADDRESS" << to_hstring(device_address) << "IS ALREADY PAIRED");
        co_return - 1;
    }

    if (!pair_state.CanPair()) {
        WIN_LOG(L"DEVICE WITH ADDRESS" << to_hstring(device_address) << "CANNOT BE PAIRED");
        co_return - 1;
    }
    const auto co_operation = pair_state.PairAsync(DevicePairingProtectionLevel::Encryption);

    auto timeout = [timeout_in_millis, co_operation]() -> fire_and_forget {
        // wait for the operation to be completed otherwise cancel it
        co_await resume_after(std::chrono::milliseconds(timeout_in_millis));
        if (co_operation.Status() == AsyncStatus::Completed) {
            WIN_LOG(L"PAIRING TIMEOUT WAITED FOR " << timeout_in_millis << "MILLISECONDS");
            co_operation.Cancel();
        }
    };

    try {
        // ensures the cancellation called it made
        timeout();
        // now call the operation
        const auto result = co_await co_operation;
        const auto status = static_cast<int32_t>(result.Status());
        WIN_LOG(L"PAIRING STATUS_CODE: " << status);
        co_return status;
    } catch (const hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WINRT EXCEPTION WHILE CHECKING BOND STATE HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                      << ex.message().c_str());
        co_return RESPONSE_ERROR_OPERATION_CANCELLED;
    } catch (...) {
        WIN_LOG(L"UNKNOWN EXCEPTION OCCURRED");
    }
    co_return RESPONSE_ERROR_UNKNOWN;
}

IAsyncOperation<bool> bluetooth_caller::is_bluetooth_active() {
    init_apartment();

    try {
        if (!m_selected_bt_radio) {
            if (const auto accessStatus = co_await Radio::RequestAccessAsync();
                accessStatus != RadioAccessStatus::Allowed) {
                WIN_LOG(L"Access denied. Check Windows Privacy Settings");
                co_return false;
            }

            const auto radios = co_await Radio::GetRadiosAsync();
            Radio local_bt_radio{nullptr};

            for (auto&& r : radios) {
                if (r.Kind() == RadioKind::Bluetooth) {
                    local_bt_radio = r;
                    break;
                }
            }

            if (!local_bt_radio) {
                WIN_LOG(L"No Bluetooth hardware radio found on this device.");
                co_return false;
            }

            {
                std::lock_guard lock(m_mutex);
                if (!m_selected_bt_radio) {
                    m_selected_bt_radio = local_bt_radio;
                }
            }
        }

        RadioState state;
        {
            std::lock_guard lock(m_mutex);
            if (!m_selected_bt_radio) co_return false;
            state = m_selected_bt_radio.State();
        }
        WIN_LOG(L"READING BLUETOOTH STATE IS_ACTIVE: " << static_cast<unsigned int>(state));

        co_return state == RadioState::On;
    } catch (const hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WinRT Exception caught in Bluetooth initialization. HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                                << ex.message().c_str());
        co_return false;
    } catch (const std::exception& ex) {
        // Catch standard library exceptions
        WIN_LOG(L"Standard exception caught in Bluetooth initialization" << ex.what());
        co_return false;
    } catch (...) {
        // Catch-all safety net
        WIN_LOG(L"Unknown critical exception caught during Bluetooth initialization.");
        co_return false;
    }
}

IAsyncAction bluetooth_caller::register_bt_listener(const std::function<void(bool)>& callback) {
    init_apartment();
    {
        std::lock_guard lock(m_mutex);

        if (callback == nullptr) {
            co_return;
        }

        if (m_selected_bt_radio) {
            try {
                if (m_eventToken.value != 0) {
                    WIN_LOG("CALLBACK ALREADY REGISTER REVOKING IT");
                    // cancels the associated state change callback and reset the event token
                    m_selected_bt_radio.StateChanged(m_eventToken);
                    m_eventToken = {0};
                }
                m_selected_bt_radio = nullptr;
            } catch (const hresult_error& ex) {
                WIN_LOG(L"WinRT Revoke Failed: " << ex.message().c_str());
            }
        }
    }

    try {
        if (!m_selected_bt_radio) {
            if (const auto accessStatus = co_await Radio::RequestAccessAsync();
                accessStatus != RadioAccessStatus::Allowed) {
                WIN_LOG(L"Access denied. Check Windows Privacy Settings");
                co_return;
            }

            for (const auto radios = co_await Radio::GetRadiosAsync(); auto&& r : radios) {
                if (r.Kind() == RadioKind::Bluetooth) {
                    m_selected_bt_radio = r;
                    break;
                }
            }
        }

        // update the on status change with the new callback
        {
            std::lock_guard lock(m_mutex);
            m_onStatusChange = callback;

            m_eventToken = m_selected_bt_radio.StateChanged([this](Radio const& sender, auto const&) {
                const bool isOn = sender.State() == RadioState::On;
                if (m_onStatusChange) {
                    m_onStatusChange(isOn);
                }
            });
        }
    } catch (const hresult_error& ex) {
        WIN_LOG(L"Register Failed: " << ex.message().c_str());
    }
}

void bluetooth_caller::unregister_bt_listener() {
    std::lock_guard lock(m_mutex);

    if (m_selected_bt_radio) {
        try {
            if (m_eventToken.value != 0) {
                m_selected_bt_radio.StateChanged(m_eventToken);
                m_eventToken = {0};
                WIN_LOG("WINRT EVENT REVOKED");
            }
            m_selected_bt_radio = nullptr;
        } catch (const hresult_error& ex) {
            WIN_LOG(L"WinRT Revoke Failed: " << ex.message().c_str());
        }
    }

    m_onStatusChange = nullptr;
    WIN_LOG("CALLBACK REMOVED");
}
