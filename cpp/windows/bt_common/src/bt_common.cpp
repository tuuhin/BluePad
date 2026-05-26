#include "bluetooth_caller.h"
#include "jni.h"
#include <memory>

using namespace std;
using namespace winrt::Windows::Foundation::Collections;

extern "C" {

JavaVM* vm_ref                                     = nullptr;
jobject m_callbackRef                              = nullptr;
static std::unique_ptr<bluetooth_caller> g_btCalls = nullptr;

jint JNI_OnLoad(JavaVM* vm, void*) {
    vm_ref    = vm;
    g_btCalls = std::make_unique<bluetooth_caller>();
    return JNI_VERSION_1_6;
}

void throw_runtime_jave_exception(JNIEnv* env, const string& message) {
    jclass exClass = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(exClass, message.c_str());
}

// helper to check if low energy connection accepted
JNIEXPORT jboolean JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_nativeIsLeSecureConnectionAvailable(JNIEnv*,
                                                                                                              jobject) {
    return bluetooth_caller::is_ble_secure_connection_available().get();
}

// helper to check if peripheral role is supported
JNIEXPORT jboolean JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_nativeIsPeripheralRoleSupported(JNIEnv*,
                                                                                                          jobject) {
    return bluetooth_caller::is_peripheral_role_supported().get();
}

JNIEXPORT jboolean JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_isDeviceBonded(JNIEnv* env, jobject,
                                                                                         jstring device_address) {

    const auto deviceAddress = env->GetStringUTFChars(device_address, nullptr);
    const auto device        = bluetooth_caller::is_device_paired(deviceAddress).get();
    env->ReleaseStringUTFChars(device_address, deviceAddress);
    return device;
}

JNIEXPORT jboolean JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_nativeIsBluetoothActive(JNIEnv* env,
                                                                                                  jobject) {
    if (!g_btCalls->is_bluetooth_active().get()) {
        throw_runtime_jave_exception(env, "BLUETOOTH ERROR");
        return false;
    }
    return true;
}

JNIEXPORT void JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_nativeRegisterBTListener(JNIEnv* env, jobject,
                                                                                               jobject callback) {
    if (m_callbackRef) {
        // callback is present so delete the callback and create a new one
        env->DeleteGlobalRef(m_callbackRef);
    }
    m_callbackRef = env->NewGlobalRef(callback);

    g_btCalls->register_bt_listener([callbackRef = m_callbackRef, vm = vm_ref](const bool isOn) {
        JNIEnv* threadEnv = nullptr;
        bool didAttach    = false;

        if (vm->GetEnv(reinterpret_cast<void**>(&threadEnv), JNI_VERSION_1_6) != JNI_OK) {
            vm->AttachCurrentThread(reinterpret_cast<void**>(&threadEnv), nullptr);
            didAttach = true;
        }
        jclass clz    = threadEnv->GetObjectClass(callbackRef);
        jmethodID mid = threadEnv->GetMethodID(clz, "onStatusChange", "(Z)V");
        threadEnv->CallVoidMethod(callbackRef, mid, isOn);
        if (didAttach)
            vm->DetachCurrentThread();
    });
}

JNIEXPORT void JNICALL Java_com_sam_ble_1common_BluetoothInfoProvider_unregisterBTListener(JNIEnv* env, jobject) {
    // unregister the callback
    g_btCalls->unregister_bt_listener();
    // remove the global ref
    if (m_callbackRef) {
        env->DeleteGlobalRef(m_callbackRef);
        m_callbackRef = nullptr;
    }
}
}
