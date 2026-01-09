#include "library.h"

#include "jni.h"
#include <winrt/Windows.Foundation.h>
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

#define WIN_LOG(msg) wclog << "[NATIVE-WINDOWS] " << msg << endl;

struct NativeContext {
    // service provider
    GattServiceProvider service_provider = nullptr;
    // characteristics
    map <wstring, GattLocalCharacteristic> characteristics;
    // callback
    jobject callback = nullptr;
    // jvm ref
    JavaVM *vm_ref = nullptr;
};

extern "C" {
// helper to check if low energy connection accepted
JNIEXPORT jboolean
JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeIsLeSecureConnectionAvailable(JNIEnv *, jobject) {
    const auto adapter = BluetoothAdapter::GetDefaultAsync().get();
    return adapter.AreLowEnergySecureConnectionsSupported();
}

// helper to check if peripheral role is supported
JNIEXPORT jboolean
JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeIsPeripheralRoleSupported(JNIEnv *, jobject) {
    const auto adapter = BluetoothAdapter::GetDefaultAsync().get();
    return adapter.IsPeripheralRoleSupported();
}

JNIEXPORT jlong
JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeCreate(JNIEnv *env, jobject) {
    init_apartment();
    auto *ctx = new NativeContext();
    env->GetJavaVM(&ctx->vm_ref);
    WIN_LOG("CONTEXT ADDED");
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeDestroy(JNIEnv * env , jobject obj, jlong
id ) {
WIN_LOG("NATIVE DESTROY CALLED") ;
auto *ctx = reinterpret_cast<NativeContext *>(id);
if ( ! ctx ) {
WIN_LOG("INVALID CONTEXT PROVIDED") ;
return ;
}
try {
if (ctx->service_provider) {
WIN_LOG("STOPPING SERVICE PROVIDERS IN DESTROY");
auto current_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(L"ADVERTISEMENT STATUS (Status :" << std::to_wstring(static_cast<int32_t>(current_status))
                                          << L")");
if (current_status == GattServiceProviderAdvertisementStatus::Started || current_status ==
GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
ctx->service_provider.

StopAdvertising();

WIN_LOG("ADVERTISING STOPPED SUCCESSFULLY");
auto updated_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(
        L"ADVERTISEMENT STATUS (Status :" << std::to_wstring(static_cast<int32_t>(updated_status))
                                          << L")");
}
}
} catch (
hresult_error const &ex
) {
WIN_LOG(L"WinRT Exception: " << ex.message().c_str());
} catch (...) {
WIN_LOG(L"SOME ERROR");
}


if (ctx->callback) {
env->
DeleteGlobalRef(ctx
->callback);
WIN_LOG("CALLBACK REMOVED");
}

if (!ctx->characteristics.

empty()

) {
ctx->characteristics.

clear();

WIN_LOG("CLEANING UP CHARACTERISTICS");
}
WIN_LOG("DETETING NATIVE CONTEXT");
delete
ctx;
WIN_LOG("NATIVE CONTEXT DESTROYED");
}

JNIEXPORT void JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeRegisterCallback(JNIEnv
*env, jobject,
jlong h, jobject
cb) {
auto *ctx = reinterpret_cast<NativeContext *>(h);
if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return;
}
ctx->
callback = env->NewGlobalRef(cb);
WIN_LOG("CALLBACK SET");
}

JNIEXPORT jint
JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeGetAdvertisingStatus(
        JNIEnv * , jobject, jlong
h) {
const auto *ctx = reinterpret_cast<NativeContext *>(h);

if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return -1;
}
return static_cast
<jint>(ctx
->service_provider.

AdvertisementStatus()

);
}

JNIEXPORT void JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeStopAdvertising(JNIEnv
*, jobject,
jlong h
) {
const auto *ctx = reinterpret_cast<NativeContext *>(h);
if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return;
}
if (ctx->service_provider) {
WIN_LOG(L"SERVICE PROVIDER IS NOT SET IN ON STOP ADVERTISING");
return;
}

try {
WIN_LOG("STOPPING SERVICE PROVIDERS ON STOP ADVERTISEMENT");
auto current_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(
        L"CURRENT ADVERTISEMENT STATUS (Status :"
                << std::to_wstring(static_cast<int32_t>(current_status)) << L")");
if (current_status == GattServiceProviderAdvertisementStatus::Started || current_status ==
GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
ctx->service_provider.

StopAdvertising();

WIN_LOG("ADVERTISING STOPPED SUCCESSFULLY");
auto updated_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(
        L"CURRENT ADVERTISEMENT STATUS (Status :"
                << std::to_wstring(static_cast<int32_t>(updated_status)) <<
                L")");
}
} catch (
hresult_error const &ex
) {
WIN_LOG(L"WinRT Exception: " << ex.message().c_str());
} catch (...) {
WIN_LOG(L"SOME ERROR");
}
}

JNIEXPORT void JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeStartAdvertising(
        JNIEnv
*env, jobject,
jlong h, jobject
config) {
auto *ctx = reinterpret_cast<NativeContext *>(h);
if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return;
}
GattServiceProviderAdvertisingParameters params;

jclass configClass = env->GetObjectClass(config);
jfieldID discoverableId = env->GetFieldID(configClass, "discoverable", "Z");
jfieldID connectableId = env->GetFieldID(configClass, "connectable", "Z");
jfieldID serviceDataId = env->GetFieldID(configClass, "serviceData", "[B");

jboolean discoverable = env->GetBooleanField(config, discoverableId);
jboolean connectable = env->GetBooleanField(config, connectableId);
jbyteArray serviceDataArray = (jbyteArray) env->GetObjectField(config, serviceDataId);

params.
IsConnectable(connectable);
params.
IsDiscoverable(discoverable);

if (serviceDataArray != nullptr) {
jsize len = env->GetArrayLength(serviceDataArray);
jbyte *buffer = env->GetByteArrayElements(serviceDataArray, nullptr);

DataWriter writer;
writer.
WriteBytes(winrt::array_view<uint8_t const>(reinterpret_cast<uint8_t *>(buffer),
        reinterpret_cast<uint8_t *>(buffer) + len)
);
IBuffer _iBuffer = writer.DetachBuffer();
params.
ServiceData(_iBuffer);

env->
ReleaseByteArrayElements(serviceDataArray, buffer, JNI_ABORT
);
}

ctx->service_provider.
StartAdvertising(params);
auto current_status = ctx->service_provider.AdvertisementStatus();
auto str = std::to_wstring(static_cast<int32_t>(current_status));
WIN_LOG(L"ADVERTISEMENT STARTED:  STATUS (Status :" << str << L")");
}


JNIEXPORT void JNICALL
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeAddService(
        JNIEnv
*env, jobject,
jlong h, jobject
jService) {
auto *ctx = reinterpret_cast<NativeContext *>(h);
if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return;
}

jclass svcCls = env->GetObjectClass(jService);

jmethodID midUuid = env->GetMethodID(svcCls, "uuid", "()Ljava/lang/String;");
jmethodID midChars = env->GetMethodID(svcCls, "characteristics", "()Ljava/util/List;");

auto jSvcUuid = (jstring) env->CallObjectMethod(jService, midUuid);
auto jCharList = env->CallObjectMethod(jService, midChars);

const char *svcUuidStr = env->GetStringUTFChars(jSvcUuid, nullptr);
std::wstring serviceUuidW = to_hstring(svcUuidStr).c_str();
env->
ReleaseStringUTFChars(jSvcUuid, svcUuidStr
);

jclass cbCls = env->GetObjectClass(ctx->callback);
jmethodID onServiceAdded = env->GetMethodID(cbCls, "onServiceAdded",
        "(Ljava/lang/String;ZI)V");


auto result = GattServiceProvider::CreateAsync(guid(serviceUuidW)).get();
env->
CallVoidMethod(ctx
->callback, onServiceAdded, jSvcUuid, result.

Error()

== BluetoothError::Success,
static_cast
<int32_t>(result
.

Error()

));

if (result.

Error()

!= BluetoothError::Success) return;

ctx->
service_provider = result.ServiceProvider();
WIN_LOG("SERVICE CREATED AND SERVICE PROVIDER ADDED");
auto current_status = ctx->service_provider.AdvertisementStatus();
auto str = std::to_wstring(static_cast<int32_t>(current_status));
WIN_LOG(L"ADVERTISEMENT STARTED:  STATUS (Status :" << str << L")");

// Iterate characteristics
jclass listCls = env->FindClass("java/util/List");
jmethodID sizeMid = env->GetMethodID(listCls, "size", "()I");
jmethodID getMid = env->GetMethodID(listCls, "get", "(I)Ljava/lang/Object;");

jint count = env->CallIntMethod(jCharList, sizeMid);

for (
int i = 0;
i<count;
i++) {
jobject jChar = env->CallObjectMethod(jCharList, getMid, i);

jclass charCls = env->GetObjectClass(jChar);
jmethodID midCharUuid = env->GetMethodID(charCls, "uuid", "()Ljava/lang/String;");
jmethodID midCanRead = env->GetMethodID(charCls, "canRead", "()Z");

auto jCharUuid = (jstring) env->CallObjectMethod(jChar, midCharUuid);
bool canRead = env->CallBooleanMethod(jChar, midCanRead);

const char *charUuidStr = env->GetStringUTFChars(jCharUuid, nullptr);
std::wstring charUuidW = to_hstring(charUuidStr).c_str();
env->
ReleaseStringUTFChars(jCharUuid, charUuidStr
);

GattLocalCharacteristicParameters params;
if (canRead)params.
CharacteristicProperties(GattCharacteristicProperties::Read);

params.
ReadProtectionLevel(GattProtectionLevel::Plain);

auto charResult = ctx->service_provider.Service().CreateCharacteristicAsync(guid(charUuidW), params).get();

auto characteristic = charResult.Characteristic();
ctx->characteristics.
insert(pair(charUuidW, characteristic)
);

// log statement
hstring char_uuid_string = to_hstring(characteristic.Uuid());
auto message = "ADDING SERVICE ADDED" + to_string(char_uuid_string);
WIN_LOG(message.c_str());

// ---------- ReadRequested Handler ----------
characteristic.ReadRequested(
[ctx, charUuidW, serviceUuidW](
GattLocalCharacteristic const &characteristic,
        GattReadRequestedEventArgs
const &args) {
auto deferral = args.GetDeferral();
auto request = args.GetRequestAsync().get();

JNIEnv *env = nullptr;
bool didAttach = false;

if (ctx->vm_ref->GetEnv(reinterpret_cast
<void **>(&env), JNI_VERSION_1_6
) != JNI_OK) {
ctx->vm_ref->AttachCurrentThread(reinterpret_cast
<void **>(&env), nullptr
);
didAttach = true;
}

jclass cbCls = env->GetObjectClass(ctx->callback);
jmethodID midRead = env->GetMethodID(cbCls, "onReadCharacteristics",
        "(Ljava/lang/String;Ljava/lang/String;)[B");

jstring jSvcUuidStr = env->NewStringUTF(winrt::to_string(serviceUuidW).c_str());
jstring jCharUuidStr = env->NewStringUTF(winrt::to_string(charUuidW).c_str());

jbyteArray result = (jbyteArray) env->CallObjectMethod(ctx->callback, midRead, jSvcUuidStr,
        jCharUuidStr);

env->
DeleteLocalRef(jSvcUuidStr);
env->
DeleteLocalRef(jCharUuidStr);

hstring char_uuid_string = to_hstring(characteristic.Uuid());
auto message = "READ REQUESTED" + to_string(char_uuid_string);
WIN_LOG(message.c_str());

if (result == nullptr) {
DataWriter w;
request.
RespondWithValue(w
.

DetachBuffer()

);
auto send_message = "CHARACTERISTIC " + to_string(char_uuid_string) + "DATA SEND NONE";
WIN_LOG(send_message.c_str());
} else {
jsize len = env->GetArrayLength(result);
jbyte *data = env->GetByteArrayElements(result, nullptr);

DataWriter w;
w.
WriteBytes(array_view<const uint8_t>(reinterpret_cast<uint8_t *>(data),
        reinterpret_cast<uint8_t *>(data) + len)
);

env->
ReleaseByteArrayElements(result, data, JNI_ABORT
);
request.
RespondWithValue(w
.

DetachBuffer()

);

std::string send_string_data(reinterpret_cast<char *>(data), len);
auto send_message = "CHARACTERISTIC " + to_string(char_uuid_string) + send_string_data;
WIN_LOG(send_message.c_str());
}
deferral.

Complete();

if (didAttach)ctx->vm_ref->

DetachCurrentThread();

});

env->
DeleteLocalRef(jChar);
env->
DeleteLocalRef(jCharUuid);
}

ctx->service_provider.AdvertisementStatusChanged(
[ctx](GattServiceProvider const &,
GattServiceProviderAdvertisementStatusChangedEventArgs const &args
) {
JNIEnv *env = nullptr;
bool didAttach = false;

if (ctx->vm_ref->GetEnv(reinterpret_cast
<void **>(&env), JNI_VERSION_1_6
) != JNI_OK) {
ctx->vm_ref->AttachCurrentThread(reinterpret_cast
<void **>(&env), nullptr
);
didAttach = true;
}

jclass cbCls = env->GetObjectClass(ctx->callback);
jmethodID midStatus = env->GetMethodID(cbCls, "onServiceStatusChange", "(I)V");

env->
CallVoidMethod(ctx
->callback, midStatus,
static_cast
<int>(args
.

Status()

));

if (didAttach) ctx->vm_ref->

DetachCurrentThread();

});
}
}
