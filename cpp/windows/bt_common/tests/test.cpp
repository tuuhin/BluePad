
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Devices.Radios.h>
#include <iostream>

using namespace std;
using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Radios;

void TEST_LOG(const std::string &msg) {
    std::cout << "[TEST] " << msg << std::endl;
}


int bluetooth_test_listener() {
    init_apartment();

    try {
        // 2. We must use .get() in a CLI tool main() to wait for async results
        // because main doesn't support co_await directly.
        auto accessStatus = Windows::Devices::Radios::Radio::RequestAccessAsync().get();

        if (accessStatus != Windows::Devices::Radios::RadioAccessStatus::Allowed) {
            std::cout << "[ERROR] Access denied. Check Windows Privacy Settings > Radios."
                      << std::endl;
            return 1;
        }

        auto radios = Windows::Devices::Radios::Radio::GetRadiosAsync().get();
        Windows::Devices::Radios::Radio btRadio{nullptr};

        for (auto &&r: radios) {
            if (r.Kind() == Windows::Devices::Radios::RadioKind::Bluetooth) {
                btRadio = r;
                break;
            }
        }

        if (!btRadio) {
            std::cout << "[ERROR] No Bluetooth hardware found." << std::endl;
            return 1;
        }

        // 3. Initial Check
        std::cout << "[INFO] Monitoring Bluetooth..." << std::endl;
        std::cout << "[STATUS] Bluetooth is currently: "
                  << (btRadio.State() == RadioState::On ? "ON" : "OFF") << std::endl;

        // 4. Set the Listener
        // Note: The event_token keeps the connection alive.
        auto token = btRadio.StateChanged([](Radio const &sender, auto const &) {
            bool isOn = (sender.State() == Windows::Devices::Radios::RadioState::On);
            std::cout << "\n[EVENT] Bluetooth Toggled: " << (isOn ? "ACTIVE" : "INACTIVE")
                      << std::endl;
        });

        std::cout << "Press [ENTER] to stop monitoring and exit." << std::endl;
        std::cin.get();

        // Cleanup
        btRadio.StateChanged(token);

    } catch (winrt::hresult_error const &ex) {
        // This captures the "Abort" reason
        std::wcerr << L"WinRT Error: " << ex.message().c_str() << L" (0x" << std::hex << ex.code()
                   << L")" << std::endl;
    }

    return 0;
}

int main(int argc, char *argv[]) {
    bluetooth_test_listener();
}
