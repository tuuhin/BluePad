#include <mutex>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Enumeration.h>
#include <winrt/Windows.Foundation.Collections.h>

#include "bluetooth_caller.h"
#include "bluetooth_enums.h"
#include "utils.h"

using namespace winrt::Windows::Devices::Bluetooth::Advertisement;
using namespace winrt::Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace winrt::Windows::Storage::Streams;
using namespace winrt::Windows::Foundation::Collections;
using namespace winrt::Windows::Devices::Bluetooth;
using namespace winrt::Windows::Devices::Radios;
using namespace winrt::Windows::Devices::Enumeration;

IAsyncOperation<bool> bluetooth_caller::is_ble_secure_connection_available() {
    winrt::init_apartment();
    try {
        const auto adapter     = co_await BluetoothAdapter::GetDefaultAsync();
        const auto isSupported = adapter.AreLowEnergySecureConnectionsSupported();
        WIN_LOG(L"IS LE CONNECTION SUPPORTED " << isSupported);
        co_return isSupported;
    } catch (const winrt::hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
    co_return false;
}

IAsyncOperation<bool> bluetooth_caller::is_peripheral_role_supported() {
    winrt::init_apartment();
    try {
        const auto adapter     = co_await BluetoothAdapter::GetDefaultAsync();
        const auto isSupported = adapter.IsPeripheralRoleSupported();
        WIN_LOG(L"LE PERIPHERAL ROLE SUPPORTED" << isSupported);
        co_return isSupported;
    } catch (const winrt::hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
    co_return false;
}

IAsyncOperation<bool> bluetooth_caller::is_bluetooth_active() {
    winrt::init_apartment();

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
    } catch (const winrt::hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WinRT Exception caught in Bluetooth initialization. HRESULT:" << ex.code().value << L"MESSAGE: "
                                                                                << ex.message().c_str());
        co_return false;
    } catch (const std::exception& ex) {
        // Catch standard library exceptions
        WIN_LOG(L"Standard exception caught in Bluetooth initialization" << ex.what());
        co_return false;
    } catch (...) {
        utils::show_stacktrace();
        // Catch-all safety net
        WIN_LOG(L"Unknown critical exception caught during Bluetooth initialization.");
        co_return false;
    }
}

IAsyncAction bluetooth_caller::register_bt_listener(const std::function<void(bool)>& callback) {
    winrt::init_apartment();
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
            } catch (const winrt::hresult_error& ex) {
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
    } catch (const winrt::hresult_error& ex) {
        WIN_LOG(L"Register Failed: " << ex.message().c_str());
    } catch (...) {
        // Catch-all safety net
        utils::show_stacktrace();
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
        } catch (const winrt::hresult_error& ex) {
            WIN_LOG(L"WinRT Revoke Failed: " << ex.message().c_str());
        }
    }

    m_onStatusChange = nullptr;
    WIN_LOG("CALLBACK REMOVED");
}

IAsyncOperation<int32_t> bluetooth_caller::request_bluetooth_enable() {

    if (const auto status = co_await Radio::RequestAccessAsync(); status != RadioAccessStatus::Allowed) {
        WIN_LOG(L"ACCESS DENIED CANNOT ENABLE BLUETOOTH FROM HERE");
        co_return REQUEST_DENIED_PRIVACY_ISSUES;
    }

    Radio selectedRadio = nullptr;
    for (const auto radios = co_await Radio::GetRadiosAsync(); auto&& r : radios) {
        if (r.Kind() == RadioKind::Bluetooth) {
            selectedRadio = r;
            break;
        }
    }

    if (selectedRadio == nullptr) {
        WIN_LOG(L"UNABLE TO FIND THE ADAPTER");
        co_return REQUEST_DENIED_CANNOT_FIND_ADAPTER;
    }

    if (selectedRadio.State() == RadioState::On) {
        WIN_LOG(L"BLUETOOTH ALREADY ENABLED NO NEED TO ENABLE IT AGAIN");
        co_return REQUEST_NOT_NEEDED;
    }

    try {
        switch (co_await selectedRadio.SetStateAsync(RadioState::On)) {
        case RadioAccessStatus::DeniedBySystem: {
            WIN_LOG("ACCESS DENIED BY SYSTEM");
            co_return REQUEST_DENIED_BY_SYSTEM;
        }
        case RadioAccessStatus::DeniedByUser: {
            WIN_LOG("ACCESS DENIED BY USER");
            co_return REQUEST_DENIED_BY_USER;
        }
        case RadioAccessStatus::Unspecified: {
            WIN_LOG("ACCESS STATE UNKNOWN");
            co_return REQUEST_DENIED_UNKNOWN;
        }
        case RadioAccessStatus::Allowed:
            break;
        }
        WIN_LOG("REQUEST ACCEPTED BY THE USER");
        co_return REQUEST_ACCEPTED;

    } catch (const winrt::hresult_error& ex) {
        WIN_LOG(L"CANNOT SET RADIO STATE EXCEPTION: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }

    co_return REQUEST_DENIED_PRIVACY_ISSUES;
}
