#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Enumeration.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/base.h>

#include "bluetooth_bond_manager.h"
#include "bluetooth_enums.h"
#include "utils.h"

using namespace std;
using namespace winrt;
using namespace winrt::Windows::Devices::Bluetooth;

IAsyncOperation<int8_t> bluetooth_bond_manager::get_bond_state(const std::string& device_address) {
    try {
        const uint64_t bt_address = utils::parse_mac_address(device_address);

        // Read the device
        constexpr auto address_type = BluetoothAddressType::Random;
        const auto device           = co_await BluetoothLEDevice::FromBluetoothAddressAsync(bt_address, address_type);

        if (device == nullptr) {
            WIN_LOG(L"UNABLE TO READ DEVICE FROM THE GIVEN DEVICE ID " << to_hstring(device_address));
            co_return ERROR_INVALID_DEVICE;
        }

        const auto device_info = device.DeviceInformation();
        const auto pairState   = device_info.Pairing();
        co_return pairState.IsPaired() ? DEVICE_BONDED : DEVICE_NOT_BONDED;
    } catch (const hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WINRT EXCEPTION WHILE CHECKING BOND STATE CODE:" << ex.code().value << L"MESSAGE: "
                                                                   << ex.message().c_str());
    } catch (const std::exception& ex) {
        // Catch standard library exceptions
        WIN_LOG(L"CPP EXCEPTION " << ex.what());
    } catch (...) {
        utils::show_stacktrace();
    }
    co_return ERROR_UNKNOWN;
}

IAsyncAction bluetooth_bond_manager::request_and_register_bond_callback(const std::string& device_address,
                                                                        bt_bond_pairing_callback callback) {

    const uint64_t bt_address   = utils::parse_mac_address(device_address);
    constexpr auto address_type = BluetoothAddressType::Random;
    const auto device           = co_await BluetoothLEDevice::FromBluetoothAddressAsync(bt_address, address_type);

    if (device == nullptr) {
        WIN_LOG(L"UNABLE TO READ DEVICE FROM THE GIVEN DEVICE ID " << to_hstring(device_address));

        co_return;
    }

    const auto device_info = device.DeviceInformation();
    const auto pairState   = device_info.Pairing();

    if (pairState.IsPaired()) {
        WIN_LOG(L"DEVICE IS ALREADY PAIRED NO NEED TO PAIR IT AGAIN" << to_hstring(device_address));
        {
            std::lock_guard lock(m_mutex);
            callback.on_error(ERROR_DEVICE_ALREADY_BONDED);
        }
        co_return;
    }

    if (!pairState.CanPair()) {
        WIN_LOG(L"CANNOT PAIR THE GIVEN DEVICE" << to_hstring(device_address));
        {
            std::lock_guard lock(m_mutex);
            callback.on_error(ERROR_DEVICE_CANNOT_BE_BONDED);
        }
        co_return;
    }

    const auto custom_pairing = pairState.Custom();

    // Cancel previous pairing if exists
    unregister_bond_callback();

    m_custom_pairing = custom_pairing;
    m_pairing_token  = custom_pairing.PairingRequested(
        [weak_self = weak_from_this(), callback](DeviceInformationCustomPairing const&,
                                                 DevicePairingRequestedEventArgs const& args) {
            auto self = weak_self.lock();
            if (!self) return;

            switch (args.PairingKind()) {
            case DevicePairingKinds::ConfirmOnly:
                WIN_LOG(L"CONFIRM ONLY CODE REQUESTED ACCEPTING IT DIRECTLY");
                args.Accept();
                break;

            case DevicePairingKinds::ConfirmPinMatch:
                WIN_LOG(L"CONFIRM MATCH REQUESTED USER APPROVAL REQUIRED");
                {
                    const auto pin = winrt::to_string(args.Pin());
                    std::lock_guard lock(self->m_mutex);

                    auto* context = new bluetooth_bond_callback_responder(args);
                    self->m_responders.insert(context);
                    callback.on_confirm_pin(pin.c_str(), reinterpret_cast<bt_bond_responder_handle>(context));
                }
                break;

            default:
                const auto kind = static_cast<uint8_t>(args.PairingKind());
                WIN_LOG(L"DEVICE PAIRING TYPE NOT SUPPORTED REQUESTED :" << kind);
            }
        });

    WIN_LOG(L"CALLBACK HAS BEEN SET WAITING FOR RESPONSE");

    constexpr auto kinds      = DevicePairingKinds::ConfirmPinMatch;
    const auto pairing_result = co_await custom_pairing.PairAsync(kinds);

    // we are done can cancel the callback now
    unregister_bond_callback();

    {
        std::lock_guard lock(m_mutex);

        const auto final_result = static_cast<uint8_t>(pairing_result.Status());

        WIN_LOG(L"FINALLY BOND RESPONDED WITH CODE" << static_cast<uint8_t>(final_result));

        const auto connection_enum = static_cast<bt_bond_response>(final_result);
        callback.on_results(connection_enum);
    }
}

void bluetooth_bond_manager::unregister_bond_callback() {
    try {
        if (m_custom_pairing) {
            std::lock_guard lock(m_mutex);
            WIN_LOG(L"CLEARING THE REQUEST PAIRING TOKEN");
            m_custom_pairing.PairingRequested(m_pairing_token);
            m_pairing_token  = {};
            m_custom_pairing = nullptr;

            for (auto* responder : m_responders) {
                delete responder;
            }
            m_responders.clear();
        }
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WINRT EXCEPTION HRESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
}

void bluetooth_bond_manager::accept_connection(const bt_bond_responder_handle& callback_responder) {

    const auto* callback_response = static_cast<bluetooth_bond_callback_responder*>(callback_responder);
    if (!callback_response) {
        WIN_LOG(L"UNABLE TO FORM THE RESPONDER HANDLE");
        return;
    }

    {
        std::lock_guard lock(m_mutex);
        auto it = m_responders.find(const_cast<bluetooth_bond_callback_responder*>(callback_response));
        if (it != m_responders.end()) {
            m_responders.erase(it);
        }
    }

    try {
        WIN_LOG("ACCEPTING THE CONNECTION");
        callback_response->event_args.Accept();
    } catch (const hresult_error& ex) {
        // Catch specific WinRT/COM exceptions (provides HRESULT and Message)
        WIN_LOG(L"WINRT EXCEPTION WHILE ACCEPTING CONNECTION:" << ex.code().value << L"MESSAGE: "
                                                               << ex.message().c_str());
    } catch (const std::exception& ex) {
        // Catch standard library exceptions
        WIN_LOG(L"CPP EXCEPTION " << ex.what());
    } catch (...) {
        utils::show_stacktrace();
    }

    // always delete the response context
    delete callback_response;
}
void bluetooth_bond_manager::cancel_connection(const bt_bond_responder_handle& callback_responder) {
    const auto* callback_response = static_cast<bluetooth_bond_callback_responder*>(callback_responder);
    if (!callback_response) {
        WIN_LOG(L"UNABLE TO FORM THE RESPONDER HANDLE");
        return;
    }

    {
        std::lock_guard lock(m_mutex);
        auto it = m_responders.find(const_cast<bluetooth_bond_callback_responder*>(callback_response));
        if (it != m_responders.end()) {
            m_responders.erase(it);
        }
    }

    // always delete the response context
    delete callback_response;
}
