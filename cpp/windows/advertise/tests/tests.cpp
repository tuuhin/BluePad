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

guid serviceUuid{0x11223344, 0x5566, 0x7788, {0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x00}};
guid charUuid{0xAABBCCDD, 0xEEFF, 0x1122, {0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0x00}};

constexpr uint16_t
DISCOVERY_SERVICE_ID = 0xFEE7; // U1
constexpr uint16_t
DATA_SERVICE_ID = 0xFEE8; // U2
guid U1 = BluetoothUuidHelper::FromShortId(DISCOVERY_SERVICE_ID);
guid U2 = BluetoothUuidHelper::FromShortId(DATA_SERVICE_ID);

IBuffer StringToIBuffer(const std::string &text) {
    DataWriter writer;
    writer.UnicodeEncoding(UnicodeEncoding::Utf8);

    writer.WriteBytes(array_view<const uint8_t>(
            reinterpret_cast<const uint8_t *>(text.data()),
            reinterpret_cast<const uint8_t *>(text.data()) + text.size()
    ));

    return writer.DetachBuffer();
}

int ble_advertisement() {
    init_apartment();
    TEST_LOG("Initializing Publisher...");
    std::cout << "[TEST] Initializing Minimal Publisher..." << std::endl;

    auto adapter = BluetoothAdapter::GetDefaultAsync().get();
    auto features = adapter.AreLowEnergySecureConnectionsSupported();
    auto isSupported = adapter.IsPeripheralRoleSupported();
    TEST_LOG("FEATURE PRESENT : " + to_string(features));
    TEST_LOG("FEATURE ROLE OKE :" + to_string(isSupported));

    if (!features || !isSupported) return 1;

    try {
        auto providedResult = GattServiceProvider::CreateAsync(serviceUuid).get();
        if (providedResult.Error() != BluetoothError::Success) {
            TEST_LOG("FAILED TO INIT GAT SERVICE");
            return 1;
        }
        auto provider = providedResult.ServiceProvider();


        GattLocalCharacteristicParameters params;
        params.CharacteristicProperties(GattCharacteristicProperties::Read);
        params.ReadProtectionLevel(GattProtectionLevel::Plain);
        params.WriteProtectionLevel(GattProtectionLevel::Plain);

        auto charResult = provider.Service().CreateCharacteristicAsync(charUuid, params).get();
        auto characteristic = charResult.Characteristic();


        characteristic.ReadRequested([](GattLocalCharacteristic const &sender,
                GattReadRequestedEventArgs const &args) {
            auto deferral = args.GetDeferral();
            auto req = args.GetRequestAsync().get();


            DataWriter w;
            w.WriteString(L"Hello from Windows BLE");
            req.RespondWithValue(w.DetachBuffer());
            deferral.Complete();
        });

        GattServiceProviderAdvertisingParameters p;
        p.ServiceData(StringToIBuffer("Hello"));
        p.IsConnectable(true);
        p.IsDiscoverable(false);
        provider.StartAdvertising(p);

        std::this_thread::sleep_for(std::chrono::milliseconds(10 * 1000));
        provider.StopAdvertising();
        TEST_LOG("ADVERTISEMENT STOPPED");
    } catch (const hresult_error &ex) {
        std::wcerr << L"WinRT Error: " << ex.message().c_str() << L" (0x" << std::hex << ex.code()
                   << L")" << std::endl;
        return 1;
    } catch (const std::exception &ex) {
        std::cerr << "Standard Error: " << ex.what() << std::endl;
        return 1;
    }
    TEST_LOG("DONE");
    return 0;
}

int main(int argc, char *argv[]) {
    ble_advertisement();
    return 0;
}
