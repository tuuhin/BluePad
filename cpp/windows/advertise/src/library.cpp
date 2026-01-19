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

void throw_runtime_exception(JNIEnv *env, const char *msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != nullptr) env->ThrowNew(cls, msg);
    env->DeleteLocalRef(cls);
}

extern "C" {

JNIEXPORT jlong
JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeCreate(JNIEnv *env, jobject) {
    init_apartment();
    // ReSharper disable once CppDFAMemoryLeak
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
WIN_LOG(L"ADVERTISEMENT STATUS :" << std::to_wstring(static_cast<int32_t>(current_status)));
if (current_status == GattServiceProviderAdvertisementStatus::Started || current_status ==
GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
ctx->service_provider.

StopAdvertising();

WIN_LOG("ADVERTISING STOPPED SUCCESSFULLY");
auto updated_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(L"ADVERTISEMENT STATUS:" << std::to_wstring(static_cast<int32_t>(updated_status)));
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
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeRegisterCallback(
        JNIEnv
*env, jobject,
const jlong h, jobject
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
        JNIEnv * , jobject,
const jlong h
) {
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
Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeStopAdvertising(
        JNIEnv
*, jobject,
const jlong h
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
const jlong h, jobject
config) {
const auto *ctx = reinterpret_cast<NativeContext *>(h);
if (!ctx) {
WIN_LOG("INVALID CONTEXT PROVIDED");
return;
}
const GattServiceProviderAdvertisingParameters params;

jclass configClass = env->GetObjectClass(config);
jfieldID discoverableId = env->GetFieldID(configClass, "discoverable", "Z");
jfieldID connectableId = env->GetFieldID(configClass, "connectable", "Z");
jfieldID serviceDataId = env->GetFieldID(configClass, "serviceData", "[B");

const jboolean discoverable = env->GetBooleanField(config, discoverableId);
const jboolean connectable = env->GetBooleanField(config, connectableId);
auto serviceDataArray = reinterpret_cast<jbyteArray>(env->GetObjectField(config, serviceDataId));

params.
IsConnectable(connectable);
params.
IsDiscoverable(discoverable);

if (serviceDataArray != nullptr) {
const jsize len = env->GetArrayLength(serviceDataArray);
jbyte *buffer = env->GetByteArrayElements(serviceDataArray, nullptr);

const DataWriter writer;
writer.
WriteBytes(winrt::array_view<uint8_t const>(reinterpret_cast<uint8_t *>(buffer),
        reinterpret_cast<uint8_t *>(buffer) + len)
);
const IBuffer _iBuffer = writer.DetachBuffer();
params.
ServiceData(_iBuffer);

env->
ReleaseByteArrayElements(serviceDataArray, buffer, JNI_ABORT
);
}

ctx->service_provider.
StartAdvertising(params);
auto current_status = ctx->service_provider.AdvertisementStatus();
const auto str = std::to_wstring(static_cast<int32_t>(current_status));
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
jmethodID onServiceAdded = env->GetMethodID(cbCls, "onServiceAdded", "(Ljava/lang/String;I)V");

auto result = GattServiceProvider::CreateAsync(guid(serviceUuidW)).get();
env->
CallVoidMethod(ctx
->callback, onServiceAdded, jSvcUuid, static_cast
<int32_t>(result
.

Error()

));

if (result.

Error()

!= BluetoothError::Success) {
throw_runtime_exception(env,
"Unable to create a service");
return;
}

ctx->
service_provider = result.ServiceProvider();
WIN_LOG("SERVICE CREATED AND SERVICE PROVIDER ADDED");
auto current_status = ctx->service_provider.AdvertisementStatus();
WIN_LOG(L"ADVERTISEMENT STARTED:  STATUS :"
        << std::to_wstring(static_cast<int32_t>(current_status)));

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
jmethodID characteristic_uuid = env->GetMethodID(charCls, "uuid", "()Ljava/lang/String;");
jmethodID characteristics_property_read = env->GetMethodID(charCls, "canRead", "()Z");
jmethodID characteristics_property_write = env->GetMethodID(charCls, "canWriteRequest", "()Z");
jmethodID characteristics_property_write_no_response = env->GetMethodID(charCls, "canWriteCommand", "()Z");

auto jCharUuid = (jstring) env->CallObjectMethod(jChar, characteristic_uuid);
bool can_read = env->CallBooleanMethod(jChar, characteristics_property_read);
bool can_write = env->CallBooleanMethod(jChar, characteristics_property_write);
bool can_write_without_response = env->CallBooleanMethod(jChar, characteristics_property_write_no_response);

const char *charUuidStr = env->GetStringUTFChars(jCharUuid, nullptr);
std::wstring charUuidW = to_hstring(charUuidStr).c_str();
env->
ReleaseStringUTFChars(jCharUuid, charUuidStr
);

GattLocalCharacteristicParameters params;
auto characteristic_properties = GattCharacteristicProperties::None;

if (can_read) {
characteristic_properties |=
GattCharacteristicProperties::Read;
params.
ReadProtectionLevel(GattProtectionLevel::Plain);
}
if (can_write) {
characteristic_properties |=
GattCharacteristicProperties::Write;
params.
WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
}

if (can_write_without_response) {
characteristic_properties |=
GattCharacteristicProperties::WriteWithoutResponse;
params.
WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
}

params.
CharacteristicProperties(characteristic_properties);

auto charResult = ctx->service_provider.Service().CreateCharacteristicAsync(guid(charUuidW), params).get();

auto characteristic = charResult.Characteristic();
ctx->characteristics.
insert(pair(charUuidW, characteristic)
);

// log statement
hstring char_uuid_string = to_hstring(characteristic.Uuid());
WIN_LOG(L"CHARACTERISTICS ADDED" << to_hstring(char_uuid_string));

// ---------- READ REQUWST HANDLER ----------
characteristic.ReadRequested(
[ctx, charUuidW, serviceUuidW](
GattLocalCharacteristic const &characteristic,
        GattReadRequestedEventArgs
const &args) {
const auto deferral = args.GetDeferral();
const auto request = args.GetRequestAsync().get();
const auto deviceId = args.Session().DeviceId().Id();

WIN_LOG(L"READ REQUESTED" << to_hstring(characteristic.Uuid()));

JNIEnv *jni_env = nullptr;
bool thread_ready = false;

if (ctx->vm_ref->GetEnv(reinterpret_cast
<void **>(&jni_env), JNI_VERSION_1_6
) != JNI_OK) {
ctx->vm_ref->AttachCurrentThread(reinterpret_cast
<void **>(&jni_env), nullptr
);
thread_ready = true;
}

jclass callback_class = jni_env->GetObjectClass(ctx->callback);
jmethodID read_characteristics_method = jni_env->GetMethodID(
        callback_class, "onReadCharacteristics",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[B");


jstring external_device_address = jni_env->NewStringUTF(to_string(deviceId).c_str());
jstring service_uuid_str = jni_env->NewStringUTF(winrt::to_string(serviceUuidW).c_str());
jstring characteristics_uuid_str = jni_env->NewStringUTF(winrt::to_string(charUuidW).c_str());

const auto read_response = reinterpret_cast<jbyteArray>(
        jni_env->CallObjectMethod(
                ctx->callback, read_characteristics_method,
                external_device_address,
                service_uuid_str,
                characteristics_uuid_str
        ));

jni_env->
DeleteLocalRef(service_uuid_str);
jni_env->
DeleteLocalRef(characteristics_uuid_str);
jni_env->
DeleteLocalRef(external_device_address);
jni_env->
DeleteLocalRef(callback_class);


if (read_response == nullptr) {
request.

RespondWithProtocolError (GattProtocolError::AttributeNotFound());

WIN_LOG(L"CHARACTERISTIC " << to_hstring(characteristic.Uuid()) << "ATTRIBUTE NOT FOUND");
} else {
const jsize len = jni_env->GetArrayLength(read_response);
jbyte *data = jni_env->GetByteArrayElements(read_response, nullptr);

const DataWriter writer;
writer.
WriteBytes(array_view<const uint8_t>(reinterpret_cast<uint8_t *>(data),
        reinterpret_cast<uint8_t *>(data) + len)
);

jni_env->
ReleaseByteArrayElements(read_response, data, JNI_ABORT
);
request.
RespondWithValue(writer
.

DetachBuffer()

);

WIN_LOG(
        L"READ CHARACTERISTIC "
                << to_hstring(characteristic.Uuid())
                << "DATA SEND SIZE "
                << to_hstring(static_cast<uint8_t>(len)));
}
deferral.

Complete();

if (thread_ready)ctx->vm_ref->

DetachCurrentThread();

});


// --------------------- CHARACTERISTICS WRITE LISTENER ------------------------------------

characteristic.WriteRequested(
[ctx, charUuidW, serviceUuidW](
GattLocalCharacteristic const &characteristic,
        GattWriteRequestedEventArgs
const &args) {
const auto deferral = args.GetDeferral();
const auto request = args.GetRequestAsync().get();
const auto deviceId = args.Session().DeviceId().Id();
const auto buffer = request.Value();

WIN_LOG(
        L"WRITE REQUESTED" << to_hstring(characteristic.Uuid())
                           << L"BUFFER SIZE "
                           << to_hstring(buffer.Length()));

JNIEnv *jni_env = nullptr;
bool thread_ready = false;

if (ctx->vm_ref->GetEnv(reinterpret_cast
<void **>(&jni_env), JNI_VERSION_1_6
) != JNI_OK) {
ctx->vm_ref->AttachCurrentThread(reinterpret_cast
<void **>(&jni_env), nullptr
);
thread_ready = true;
}

jclass callback_class = jni_env->GetObjectClass(ctx->callback);
jmethodID on_write_characteristics_method = jni_env->GetMethodID(
        callback_class, "onWriteCharacteristicRequest",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)V");


jstring external_device_address = jni_env->NewStringUTF(to_string(deviceId).c_str());
jstring service_uuid_str = jni_env->NewStringUTF(winrt::to_string(serviceUuidW).c_str());
jstring characteristics_uuid_str = jni_env->NewStringUTF(winrt::to_string(charUuidW).c_str());

jbyteArray write_request_value = jni_env->NewByteArray(static_cast<jsize>(buffer.Length()));
jni_env->
SetByteArrayRegion(write_request_value,
0, static_cast
<jsize>(buffer
.

Length()

),
reinterpret_cast
<const jbyte *>(buffer
.

data()

));

jni_env->
CallObjectMethod(
        ctx
->callback,
on_write_characteristics_method,
external_device_address,
service_uuid_str,
characteristics_uuid_str, write_request_value
);

jni_env->
DeleteLocalRef(service_uuid_str);
jni_env->
DeleteLocalRef(characteristics_uuid_str);
jni_env->
DeleteLocalRef(external_device_address);
jni_env->
DeleteLocalRef(write_request_value);
jni_env->
DeleteLocalRef(callback_class);

if (request.

Option()

== GattWriteOption::WriteWithResponse) {
WIN_LOG(L"SENDING A WRITE RESPONSE");
request.

Respond();

}

deferral.

Complete();

if (thread_ready)ctx->vm_ref->

DetachCurrentThread();

});


env->
DeleteLocalRef(jChar);
env->
DeleteLocalRef(jCharUuid);
}


// --------------------- ADVERTISEMENT_STATUS_LISTENER ------------------------------------
ctx->service_provider.AdvertisementStatusChanged(
[ctx](GattServiceProvider const &,
GattServiceProviderAdvertisementStatusChangedEventArgs const &args
) {
JNIEnv *jni_env = nullptr;
bool is_thread_ready = false;

if (ctx->vm_ref->GetEnv(reinterpret_cast
<void **>(&jni_env), JNI_VERSION_1_6
) != JNI_OK) {
ctx->vm_ref->AttachCurrentThread(reinterpret_cast
<void **>(&jni_env), nullptr
);
is_thread_ready = true;
}

jclass callback_class = jni_env->GetObjectClass(ctx->callback);
jmethodID device_status_change_method = jni_env->GetMethodID(
        callback_class, "onServiceStatusChange", "(I)V");

jni_env->
CallVoidMethod(ctx
->callback, device_status_change_method, static_cast
<int>(args
.

Status()

));

if (is_thread_ready)ctx->vm_ref->

DetachCurrentThread();

});
}
}
