#include "jni.h"
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Radios.h>
#include <iostream>
#include <mutex>

using namespace std;
using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Radios;

#define WIN_LOG(msg) wclog << "[NATIVE-WINDOWS] " << msg << endl;

jobject g_callbackRef = nullptr;
event_token g_eventToken;
JavaVM *vm_ref = nullptr;
std::mutex global_mutex;
Radio selected_bt_radio{nullptr};

extern "C" {
jint JNI_OnLoad(JavaVM *vm, void *) {
    vm_ref = vm;
    return JNI_VERSION_1_6;
}


JNIEXPORT jboolean
JNICALL
Java_com_sam_ble_1common_BluetoothInfoProvider_nativeIsBluetoothActive(JNIEnv *env, jobject) {
    std::lock_guard lock(global_mutex);
    try {
        jclass exClass = env->FindClass("java/lang/RuntimeException");
        if (!selected_bt_radio) {
            auto accessStatus = Radio::RequestAccessAsync().get();
            if (accessStatus != RadioAccessStatus::Allowed) {
                WIN_LOG(L"Access denied. Check Windows Privacy Settings");
                env->ThrowNew(exClass, "ACCESS DENIED");
                return false;
            }

            const auto radios = Radio::GetRadiosAsync().get();
            for (auto &&r: radios) {
                if (r.Kind() == RadioKind::Bluetooth) {
                    selected_bt_radio = r;
                    break;
                }
            }
            if (!selected_bt_radio) {
                env->ThrowNew(exClass, "NO BLUETOOTH HARDWARE FOUND");
                return false;
            }
        }

        WIN_LOG("READING BT STATE");
        return selected_bt_radio && selected_bt_radio.State() == RadioState::On;
    } catch (...) {
        return false;
    }
}

JNIEXPORT void JNICALL
Java_com_sam_ble_1common_BluetoothInfoProvider_nativeRegisterBTListener
(JNIEnv * env , jobject , jobject callback ) {
std::lock_guard lock(global_mutex);

jclass exClass = env->FindClass("java/lang/RuntimeException");

if ( ! callback ) {
env -> ThrowNew(exClass, "Callback not provided" ) ;
return ;
}

if (selected_bt_radio) {
try {
if (g_eventToken.value != 0) {
WIN_LOG("CALLBACK ALREADY REGISTER REVOKING IT");
selected_bt_radio.
StateChanged(g_eventToken);
g_eventToken = {0};
WIN_LOG("CALLBACK REVOKED");
}
selected_bt_radio = nullptr;
} catch (
const hresult_error &ex
) {
WIN_LOG(L"WinRT Revoke Failed: " << ex.message().c_str());
}
}

try {
if (!selected_bt_radio) {
auto accessStatus = Radio::RequestAccessAsync().get();
if (accessStatus != RadioAccessStatus::Allowed) {
WIN_LOG(L"Access denied. Check Windows Privacy Settings");
env->
ThrowNew(exClass,
"ACCESS DENIED");
return;
}

const auto radios = Radio::GetRadiosAsync().get();
for (
auto &&r
: radios) {
if (r.

Kind()

== RadioKind::Bluetooth) {
selected_bt_radio = r;
break;
}
}
if (!selected_bt_radio) {
env->
ThrowNew(exClass,
"NO BLUETOOTH HARDWARE FOUND");
return;
}
WIN_LOG("BLUETOOTH RADIO ATTACHED");
}
// 3. Setup JNI Callback
if (g_callbackRef) env->
DeleteGlobalRef(g_callbackRef);
g_callbackRef = env->NewGlobalRef(callback);

g_eventToken = selected_bt_radio.StateChanged([](Radio const &sender, auto const &) {
    const bool isOn = (sender.State() == RadioState::On);
    JNIEnv * threadEnv = nullptr;
    bool didAttach = false;

    if (vm_ref->GetEnv(reinterpret_cast<void **>(&threadEnv), JNI_VERSION_1_6) != JNI_OK) {
        vm_ref->AttachCurrentThread(reinterpret_cast<void **>(&threadEnv), nullptr);
        didAttach = true;
    }
    jclass clz = threadEnv->GetObjectClass(g_callbackRef);
    jmethodID mid = threadEnv->GetMethodID(clz, "onStatusChange", "(Z)V");
    threadEnv->CallVoidMethod(g_callbackRef, mid, isOn);
    if (didAttach) vm_ref->DetachCurrentThread();
});
} catch (
const hresult_error &ex
) {
env->
ThrowNew(exClass, winrt::to_string(ex.message())
.

c_str()

);
}
}

JNIEXPORT void JNICALL
Java_com_sam_ble_1common_BluetoothInfoProvider_unregisterBTListener
(JNIEnv
*env, jobject) {
std::lock_guard lock(global_mutex);

if (selected_bt_radio) {
try {
if (g_eventToken.value != 0) {
selected_bt_radio.
StateChanged(g_eventToken);
g_eventToken = {0};
WIN_LOG("WINRT EVENT REVOKED");
}
selected_bt_radio = nullptr;
} catch (
const hresult_error &ex
) {
WIN_LOG(L"WinRT Revoke Failed: " << ex.message().c_str());
}
}

if (g_callbackRef) {
env->
DeleteGlobalRef(g_callbackRef);
g_callbackRef = nullptr;
WIN_LOG("CALLBACK REMOVED");
}

WIN_LOG("CLEAN UP DONE");
}
}
