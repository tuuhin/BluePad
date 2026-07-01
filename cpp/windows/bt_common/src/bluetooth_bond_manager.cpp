#include <memory>
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
        [self_ref = weak_from_this(), callback](DeviceInformationCustomPairing const&,
                                                DevicePairingRequestedEventArgs const& args) {
            const auto self = self_ref.lock();
            if (!self) {
                WIN_LOG(L"UNABLE TO RESOLVE THE POINTER");
                return;
            }

            switch (args.PairingKind()) {
            case DevicePairingKinds::ConfirmOnly:
                WIN_LOG(L"CONFIRM ONLY CODE REQUESTED ACCEPTING IT DIRECTLY");
                args.Accept();
                break;

            case DevicePairingKinds::ConfirmPinMatch:
                WIN_LOG(L"CONFIRM MATCH REQUESTED USER APPROVAL REQUIRED");
                {
                    auto deferral  = args.GetDeferral();
                    const auto pin = winrt::to_string(args.Pin());
                    std::lock_guard lock(self->m_mutex);

                    // Allocate via shared_ptr to prevent lifecycle crashes if PairAsync returns early
                    const auto context = std::make_shared<bluetooth_bond_callback_responder>(args, deferral);
                    self->m_responders.insert(context);

                    WIN_LOG(L"REQUESTING USER APPROVAL");

                    callback.on_confirm_pin(pin.c_str(), reinterpret_cast<bt_bond_responder_handle>(context.get()));
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
    WIN_LOG(L"PAIRING RESPONSE FOUND!!");

    unregister_bond_callback();

    {
        std::lock_guard lock(m_mutex);
        const auto final_result = static_cast<uint8_t>(pairing_result.Status());
        WIN_LOG(L"PAIRING RESPONDED WITH CODE: " << final_result);

        const auto connection_enum = static_cast<bt_bond_response>(final_result);
        callback.on_results(connection_enum);
    }
}

void bluetooth_bond_manager::unregister_bond_callback() {
    if (m_custom_pairing == nullptr) return;
    try {
        std::lock_guard lock(m_mutex);
        m_custom_pairing.PairingRequested(m_pairing_token);
        m_pairing_token  = {};
        m_custom_pairing = nullptr;

        m_responders.clear();
        WIN_LOG(L"CLEARING THE REQUEST PAIRING TOKEN");
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WINRT EXCEPTION HRESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
}

void bluetooth_bond_manager::accept_connection(const std::string& pin,
                                               const bt_bond_responder_handle& callback_responder) {

    auto* const raw_response = static_cast<bluetooth_bond_callback_responder*>(callback_responder);
    if (!raw_response) {
        WIN_LOG(L"UNABLE TO FORM THE RESPONDER HANDLE");
        return;
    }

    std::shared_ptr<bluetooth_bond_callback_responder> holder;
    {
        std::lock_guard lock(m_mutex);
        const auto it = ranges::find_if(m_responders,
                                        [raw_response](const std::shared_ptr<bluetooth_bond_callback_responder>& ptr) {
                                            return ptr.get() == raw_response;
                                        });

        if (it != m_responders.end()) {
            WIN_LOG(L"REMOVING OBJECT REFERENCES");
            holder = *it;
            m_responders.erase(it);
        }
    }

    if (holder == nullptr) {
        WIN_LOG(L"RESPONDER OBJECT ALREADY PROCESSED");
        return;
    }

    try {
        WIN_LOG("ACCEPTING THE CONNECTION WITH PIN");
        holder->event_args.Accept(to_hstring(pin));

        WIN_LOG("DEFERRAL REQUEST COMPLETED");
        holder->deferral.Complete();
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WINRT EXCEPTION WHILE ACCEPTING CONNECTION:" << ex.code().value << L"MESSAGE: "
                                                               << ex.message().c_str());
    } catch (const std::exception& ex) {
        WIN_LOG(L"CPP EXCEPTION " << ex.what());
    } catch (...) {
        utils::show_stacktrace();
    }
}

void bluetooth_bond_manager::cancel_connection(const bt_bond_responder_handle& callback_responder) {
    auto* const raw_response = static_cast<bluetooth_bond_callback_responder*>(callback_responder);
    if (!raw_response) {
        WIN_LOG(L"UNABLE TO FORM THE RESPONDER HANDLE");
        return;
    }

    std::shared_ptr<bluetooth_bond_callback_responder> holder;
    {
        std::lock_guard lock(m_mutex);
        const auto it = ranges::find_if(m_responders,
                                        [raw_response](const std::shared_ptr<bluetooth_bond_callback_responder>& ptr) {
                                            return ptr.get() == raw_response;
                                        });

        if (it != m_responders.end()) {
            WIN_LOG(L"REMOVING OBJECT REFERENCES");
            holder = *it;
            m_responders.erase(it);
        }
    }

    if (holder == nullptr) return;
    try {
        WIN_LOG("DEFERRAL REQUEST MARKED COMPLETED");
        // the deferral is closed the os now can terminate the session
        holder->deferral.Complete();
    } catch (...) {
        utils::show_stacktrace();
    }
}
