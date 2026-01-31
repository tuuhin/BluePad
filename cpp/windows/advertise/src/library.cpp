#include "library.h"

#include "jni.h"
#include <iostream>
#include <winerror.h>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Radios.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Storage.Streams.h>

using namespace std;
using namespace winrt;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Storage::Streams;
using namespace Windows::Foundation::Collections;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Radios;
using namespace Windows::Foundation;

#define WIN_LOG(msg) wclog << "[NATIVE-WINDOWS] " << msg << endl;

struct NativeContext {
    // service provider
    GattServiceProvider service_provider = nullptr;
    // characteristics
    map<wstring, GattLocalCharacteristic> characteristics;
    // callback
    jobject callback = nullptr;
    // jvm ref
    JavaVM* vm_ref = nullptr;
};

void throw_runtime_exception(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != nullptr) {
        env->ThrowNew(cls, msg);
    }
    env->DeleteLocalRef(cls);
}

extern "C" {
JNIEXPORT jlong JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeCreate(JNIEnv* env, jobject obj) {
    init_apartment();
    // ReSharper disable once CppDFAMemoryLeak
    auto* ctx = new NativeContext();
    env->GetJavaVM(&ctx->vm_ref);
    WIN_LOG("CONTEXT ADDED");
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeDestroy(JNIEnv* env, jobject, jlong id) {
    WIN_LOG("NATIVE DESTROY CALLED");
    auto* ctx = reinterpret_cast<NativeContext*>(id);
    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return;
    }
    try {
        if (ctx->service_provider) {
            WIN_LOG("STOPPING SERVICE PROVIDERS IN DESTROY");
            auto current_status = ctx->service_provider.AdvertisementStatus();
            WIN_LOG(L"ADVERTISEMENT STATUS :" << std::to_wstring(static_cast<int32_t>(current_status)));
            if (current_status == GattServiceProviderAdvertisementStatus::Started ||
                current_status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
                ctx->service_provider.StopAdvertising();
                WIN_LOG("ADVERTISING STOPPED SUCCESSFULLY");
                auto updated_status = ctx->service_provider.AdvertisementStatus();
                WIN_LOG(L"ADVERTISEMENT STATUS:" << std::to_wstring(static_cast<int32_t>(updated_status)));
            }
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WinRT Exception: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"SOME ERROR");
    }

    if (ctx->callback) {
        env->DeleteGlobalRef(ctx->callback);
        WIN_LOG("CALLBACK REMOVED");
    }

    if (!ctx->characteristics.empty()) {
        ctx->characteristics.clear();
        WIN_LOG("CLEANING UP CHARACTERISTICS");
    }
    WIN_LOG("DETETING NATIVE CONTEXT");
    delete ctx;
    WIN_LOG("NATIVE CONTEXT DESTROYED");
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeRegisterCallback(JNIEnv* env, jobject,
                                                                                          const jlong h, jobject cb) {
    auto* ctx = reinterpret_cast<NativeContext*>(h);
    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return;
    }
    ctx->callback = env->NewGlobalRef(cb);
    WIN_LOG("CALLBACK SET");
}

JNIEXPORT jint JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeGetAdvertisingStatus(JNIEnv*, jobject,
                                                                                              const jlong h) {
    const auto* ctx = reinterpret_cast<NativeContext*>(h);

    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return -1;
    }
    return static_cast<jint>(ctx->service_provider.AdvertisementStatus());
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeStopAdvertising(JNIEnv*, jobject,
                                                                                         const jlong h) {
    const auto* ctx = reinterpret_cast<NativeContext*>(h);
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
        WIN_LOG(L"CURRENT ADVERTISEMENT STATUS (Status :" << std::to_wstring(static_cast<int32_t>(current_status))
                                                          << L")");
        if (current_status == GattServiceProviderAdvertisementStatus::Started ||
            current_status == GattServiceProviderAdvertisementStatus::StartedWithoutAllAdvertisementData) {
            ctx->service_provider.StopAdvertising();
            WIN_LOG("ADVERTISING STOPPED SUCCESSFULLY");
            auto updated_status = ctx->service_provider.AdvertisementStatus();
            WIN_LOG(L"CURRENT ADVERTISEMENT STATUS (Status :" << std::to_wstring(static_cast<int32_t>(updated_status))
                                                              << L")");
        }
    } catch (hresult_error const& ex) {
        WIN_LOG(L"WinRT Exception: " << ex.message().c_str());
    } catch (...) {
        WIN_LOG(L"SOME ERROR");
    }
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeStartAdvertising(JNIEnv* env, jobject,
                                                                                          const jlong h,
                                                                                          jobject config) {
    const auto* ctx = reinterpret_cast<NativeContext*>(h);
    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return;
    }
    const GattServiceProviderAdvertisingParameters params;

    jclass configClass      = env->GetObjectClass(config);
    jfieldID discoverableId = env->GetFieldID(configClass, "discoverable", "Z");
    jfieldID connectableId  = env->GetFieldID(configClass, "connectable", "Z");
    jfieldID serviceDataId  = env->GetFieldID(configClass, "serviceData", "[B");

    const jboolean discoverable = env->GetBooleanField(config, discoverableId);
    const jboolean connectable  = env->GetBooleanField(config, connectableId);
    auto serviceDataArray       = reinterpret_cast<jbyteArray>(env->GetObjectField(config, serviceDataId));

    params.IsConnectable(connectable);
    params.IsDiscoverable(discoverable);

    if (serviceDataArray != nullptr) {
        const jsize len = env->GetArrayLength(serviceDataArray);
        jbyte* buffer   = env->GetByteArrayElements(serviceDataArray, nullptr);

        const DataWriter writer;
        writer.WriteBytes(winrt::array_view<uint8_t const>(reinterpret_cast<uint8_t*>(buffer),
                                                           reinterpret_cast<uint8_t*>(buffer) + len));
        const IBuffer _iBuffer = writer.DetachBuffer();
        params.ServiceData(_iBuffer);

        env->ReleaseByteArrayElements(serviceDataArray, buffer, JNI_ABORT);
    }

    ctx->service_provider.StartAdvertising(params);
    auto current_status = ctx->service_provider.AdvertisementStatus();
    const auto str      = std::to_wstring(static_cast<int32_t>(current_status));
    WIN_LOG(L"ADVERTISEMENT STARTED:  STATUS (Status :" << str << L")");
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeAddService(JNIEnv* env, jobject, jlong h,
                                                                                    jobject jService) {
    auto* ctx = reinterpret_cast<NativeContext*>(h);
    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return;
    }

    jclass java_service_class = env->GetObjectClass(jService);

    jmethodID service_class_uuid_method = env->GetMethodID(java_service_class, "uuid", "()Ljava/lang/String;");
    jmethodID service_class_characteristics_method =
        env->GetMethodID(java_service_class, "characteristics", "()Ljava/util/List;");

    auto service_class_uuid_value              = (jstring)env->CallObjectMethod(jService, service_class_uuid_method);
    jobject service_class_characteristics_list = env->CallObjectMethod(jService, service_class_characteristics_method);

    const char* service_class_utf_str = env->GetStringUTFChars(service_class_uuid_value, nullptr);
    std::wstring service_class_w_str  = to_hstring(service_class_utf_str).c_str();
    env->ReleaseStringUTFChars(service_class_uuid_value, service_class_utf_str);

    jclass callback_class = env->GetObjectClass(ctx->callback);
    jmethodID callback_class_on_service_added_method =
        env->GetMethodID(callback_class, "onServiceAdded", "(Ljava/lang/String;I)V");

    auto result = GattServiceProvider::CreateAsync(guid(service_class_w_str)).get();
    env->CallVoidMethod(ctx->callback, callback_class_on_service_added_method, service_class_uuid_value,
                        static_cast<int32_t>(result.Error()));

    if (result.Error() != BluetoothError::Success) {
        throw_runtime_exception(env, "Unable to create a service");
        return;
    }

    ctx->service_provider = result.ServiceProvider();
    WIN_LOG("SERVICE CREATED AND SERVICE PROVIDER ADDED");
    auto current_status = ctx->service_provider.AdvertisementStatus();

    WIN_LOG(L"ADVERTISEMENT STATUS :" << std::to_wstring(static_cast<int32_t>(current_status)));

    // Iterate characteristics
    jclass java_list_class     = env->FindClass("java/util/List");
    jmethodID list_size_method = env->GetMethodID(java_list_class, "size", "()I");
    jmethodID list_get_method  = env->GetMethodID(java_list_class, "get", "(I)Ljava/lang/Object;");

    jint count = env->CallIntMethod(service_class_characteristics_list, list_size_method);

    for (int i = 0; i < count; i++) {

        jobject characteristics_class_method =
            env->CallObjectMethod(service_class_characteristics_list, list_get_method, i);
        jclass characteristics_class         = env->GetObjectClass(characteristics_class_method);
        jmethodID characteristic_uuid_method = env->GetMethodID(characteristics_class, "uuid", "()Ljava/lang/String;");

        // properties
        jmethodID method_can_read          = env->GetMethodID(characteristics_class, "canRead", "()Z");
        jmethodID method_can_write_request = env->GetMethodID(characteristics_class, "canWriteRequest", "()Z");
        jmethodID method_can_write_command = env->GetMethodID(characteristics_class, "canWriteCommand", "()Z");
        jmethodID method_can_notify        = env->GetMethodID(characteristics_class, "canNotify", "()Z");
        jmethodID method_can_indicate      = env->GetMethodID(characteristics_class, "canIndicate", "()Z");

        // properties values
        const bool can_read         = env->CallBooleanMethod(characteristics_class_method, method_can_read);
        const bool can_write        = env->CallBooleanMethod(characteristics_class_method, method_can_write_request);
        const bool can_write_w_resp = env->CallBooleanMethod(characteristics_class_method, method_can_write_command);
        const bool can_notify       = env->CallBooleanMethod(characteristics_class_method, method_can_notify);
        const bool can_indicate     = env->CallBooleanMethod(characteristics_class_method, method_can_indicate);

        // characteristics uuid
        jstring java_characteristics_uuid_ob =
            (jstring)env->CallObjectMethod(characteristics_class_method, characteristic_uuid_method);
        const char* characteristics_uuid_utf_str = env->GetStringUTFChars(java_characteristics_uuid_ob, nullptr);
        std::wstring characteristics_uuid_w_str  = to_hstring(characteristics_uuid_utf_str).c_str();
        env->ReleaseStringUTFChars(java_characteristics_uuid_ob, characteristics_uuid_utf_str);

        GattLocalCharacteristicParameters params;
        auto characteristic_properties = GattCharacteristicProperties::None;

        if (can_read) {
            characteristic_properties |= GattCharacteristicProperties::Read;
            params.ReadProtectionLevel(GattProtectionLevel::Plain);
        }
        if (can_write) {
            characteristic_properties |= GattCharacteristicProperties::Write;
            params.WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
        }

        if (can_write_w_resp) {
            characteristic_properties |= GattCharacteristicProperties::WriteWithoutResponse;
            params.WriteProtectionLevel(GattProtectionLevel::EncryptionRequired);
        }
        if (can_notify)
            characteristic_properties |= GattCharacteristicProperties::Notify;

        if (can_indicate)
            characteristic_properties |= GattCharacteristicProperties::Indicate;

        params.CharacteristicProperties(characteristic_properties);

        auto charResult =
            ctx->service_provider.Service().CreateCharacteristicAsync(guid(characteristics_uuid_w_str), params).get();

        auto characteristic = charResult.Characteristic();
        ctx->characteristics.insert(pair(characteristics_uuid_w_str, characteristic));

        WIN_LOG(L"CHARACTERISTICS ADDED : " << characteristics_uuid_utf_str);

        // ADD DESCRIPTORS
        jmethodID midDescs = env->GetMethodID(characteristics_class, "descriptors", "()Ljava/util/List;");
        jobject jDescList  = env->CallObjectMethod(characteristics_class_method, midDescs);
        jint descCount     = env->CallIntMethod(jDescList, list_size_method);

        for (int j = 0; j < descCount; ++j) {
            jobject java_descriptor_method = env->CallObjectMethod(jDescList, list_get_method, j);
            jclass java_descriptor         = env->GetObjectClass(java_descriptor_method);

            // Get Descriptor UUID
            jmethodID j_descriptor_md = env->GetMethodID(java_descriptor, "uuid", "()Ljava/lang/String;");
            jstring j_descriptor_uuid = (jstring)env->CallObjectMethod(java_descriptor_method, j_descriptor_md);

            const char* descriptor_uuid          = env->GetStringUTFChars(j_descriptor_uuid, nullptr);
            std::wstring descriptor_uuid_wstring = to_hstring(descriptor_uuid).c_str();
            env->ReleaseStringUTFChars(j_descriptor_uuid, descriptor_uuid);

            try {
                GattLocalDescriptorParameters descriptor_params;
                // TODO: INCLUDING THE PROTECTION LEVELS AS PLAIN
                descriptor_params.ReadProtectionLevel(GattProtectionLevel::Plain);
                descriptor_params.WriteProtectionLevel(GattProtectionLevel::Plain);

                auto descriptor_result =
                    characteristic.CreateDescriptorAsync(guid(descriptor_uuid_wstring), descriptor_params).get();

                WIN_LOG(L"DESCRIPTOR ADDED : " << descriptor_uuid);

                if (descriptor_result.Error() != BluetoothError::Success) {
                    throw_runtime_exception(env, "Unable to add descriptor");
                    continue;
                }
                auto descriptor = descriptor_result.Descriptor();

                // ---------------------- READ DESCRIPTOR HANDLE ---------------------------
                descriptor.ReadRequested(
                    [ctx, service_class_w_str, characteristics_uuid_w_str,
                     descriptor_uuid_wstring](GattLocalDescriptor const& desc, GattReadRequestedEventArgs const& args) {
                        const auto deferral = args.GetDeferral();
                        const auto request  = args.GetRequestAsync().get();
                        const auto deviceId = args.Session().DeviceId().Id();

                        WIN_LOG(L"DESCRIPTOR READ REQUESTED" << to_hstring(desc.Uuid()));

                        JNIEnv* jni_env   = nullptr;
                        bool thread_ready = false;

                        if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                            ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                            thread_ready = true;
                        }

                        jclass callback_class = jni_env->GetObjectClass(ctx->callback);
                        // device;service;characteristics;descriptor -> bytearray
                        jmethodID read_descriptor_method = jni_env->GetMethodID(
                            callback_class, "onReadDescriptorRequest",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[B");

                        jstring device_address   = jni_env->NewStringUTF(to_string(deviceId).c_str());
                        jstring service_uuid_str = jni_env->NewStringUTF(winrt::to_string(service_class_w_str).c_str());
                        jstring characteristics_uuid_str =
                            jni_env->NewStringUTF(winrt::to_string(characteristics_uuid_w_str).c_str());
                        jstring descriptor_uuid_str =
                            jni_env->NewStringUTF(winrt::to_string(descriptor_uuid_wstring).c_str());

                        const auto read_response = reinterpret_cast<jbyteArray>(
                            jni_env->CallObjectMethod(ctx->callback, read_descriptor_method, device_address,
                                                      service_uuid_str, characteristics_uuid_str, descriptor_uuid_str));

                        jni_env->DeleteLocalRef(device_address);
                        jni_env->DeleteLocalRef(service_uuid_str);
                        jni_env->DeleteLocalRef(characteristics_uuid_str);
                        jni_env->DeleteLocalRef(descriptor_uuid_str);
                        jni_env->DeleteLocalRef(callback_class);

                        if (read_response == nullptr) {
                            request.RespondWithProtocolError(GattProtocolError::InvalidHandle());
                            WIN_LOG(L"DESCRIPTOR " << to_hstring(desc.Uuid()) << "NO DATA TO SEND");
                        } else {
                            const jsize len = jni_env->GetArrayLength(read_response);
                            jbyte* data     = jni_env->GetByteArrayElements(read_response, nullptr);

                            const DataWriter writer;
                            writer.WriteBytes(array_view<const uint8_t>(reinterpret_cast<uint8_t*>(data),
                                                                        reinterpret_cast<uint8_t*>(data) + len));

                            jni_env->ReleaseByteArrayElements(read_response, data, JNI_ABORT);
                            request.RespondWithValue(writer.DetachBuffer());

                            WIN_LOG(L"READ DESCRIPTOR " << to_hstring(desc.Uuid()) << "DATA SEND SIZE "
                                                        << to_hstring(static_cast<uint8_t>(len)));
                        }
                        deferral.Complete();

                        if (thread_ready)
                            ctx->vm_ref->DetachCurrentThread();
                    });

                // -------------------------- WRITE DESCRIPTOR HANDLE----------------------------------
                descriptor.WriteRequested([ctx, service_class_w_str, characteristics_uuid_w_str,
                                           descriptor_uuid_wstring](GattLocalDescriptor const& desc,
                                                                    GattWriteRequestedEventArgs const& args) {
                    const auto deferral = args.GetDeferral();
                    const auto request  = args.GetRequestAsync().get();
                    const auto deviceId = args.Session().DeviceId().Id();
                    const auto buffer   = request.Value();

                    WIN_LOG(L"WRITE REQUESTED DESCRIPTOR " << to_hstring(desc.Uuid()) << L"BUFFER SIZE "
                                                           << to_hstring(buffer.Length()));

                    JNIEnv* jni_env   = nullptr;
                    bool thread_ready = false;

                    if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                        ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                        thread_ready = true;
                    }

                    jclass callback_class = jni_env->GetObjectClass(ctx->callback);
                    // device;service;characteristics;descriptor,value -> void
                    jmethodID on_write_characteristics_method = jni_env->GetMethodID(
                        callback_class, "onWriteDescriptorRequest",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)V");

                    jstring device_address_str = jni_env->NewStringUTF(to_string(deviceId).c_str());
                    jstring service_uuid_str   = jni_env->NewStringUTF(winrt::to_string(service_class_w_str).c_str());
                    jstring characteristics_uuid_str =
                        jni_env->NewStringUTF(winrt::to_string(characteristics_uuid_w_str).c_str());
                    jstring descriptor_uuid_str =
                        jni_env->NewStringUTF(winrt::to_string(descriptor_uuid_wstring).c_str());
                    jbyteArray write_request_value = jni_env->NewByteArray(static_cast<jsize>(buffer.Length()));
                    jni_env->SetByteArrayRegion(write_request_value, 0, static_cast<jsize>(buffer.Length()),
                                                reinterpret_cast<const jbyte*>(buffer.data()));

                    jni_env->CallObjectMethod(ctx->callback, on_write_characteristics_method, device_address_str,
                                              service_uuid_str, characteristics_uuid_str, descriptor_uuid_str,
                                              write_request_value);

                    jni_env->DeleteLocalRef(device_address_str);
                    jni_env->DeleteLocalRef(service_uuid_str);
                    jni_env->DeleteLocalRef(characteristics_uuid_str);
                    jni_env->DeleteLocalRef(descriptor_uuid_str);
                    jni_env->DeleteLocalRef(write_request_value);
                    jni_env->DeleteLocalRef(callback_class);

                    deferral.Complete();

                    if (thread_ready)
                        ctx->vm_ref->DetachCurrentThread();
                });
            } catch (hresult_error const& ex) {
                const auto message = "Runtime exception" + to_string(ex.message());
                WIN_LOG(L"DESCRIPTOR REJECTED BY SYSTEM MESSAGE :" << to_hstring(message) << L"ERROR CODE"
                                                                   << ex.code());

                throw_runtime_exception(env, message.c_str());
            } catch (...) {
                throw_runtime_exception(env, "Unknown exception occurred:");
            }
        }

        // ----------------- CHECK IF SUBSCRIBED CLIENTS UPDATED
        // ReSharper disable once CppExpressionWithoutSideEffects
        characteristic.SubscribedClientsChanged([](GattLocalCharacteristic const& ch, auto const&) {
            const auto clients = ch.SubscribedClients();
            const auto characteristics_uuid_h = to_hstring(ch.Uuid());

            for (GattSubscribedClient const& client : clients) {
                auto address = client.Session().DeviceId().Id();
                WIN_LOG(L" DEVICE ADDRESS " << address << " SUBSCRIBED TO " << characteristics_uuid_h);
            }
        });

        // ---------- READ REQUEST HANDLER ----------
        characteristic.ReadRequested([ctx, characteristics_uuid_w_str,
                                      service_class_w_str](GattLocalCharacteristic const& characteristic,
                                                           GattReadRequestedEventArgs const& args) {
            const auto deferral = args.GetDeferral();
            const auto request  = args.GetRequestAsync().get();
            const auto deviceId = args.Session().DeviceId().Id();

            WIN_LOG(L"READ REQUESTED" << to_hstring(characteristic.Uuid()));

            JNIEnv* jni_env   = nullptr;
            bool thread_ready = false;

            if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                thread_ready = true;
            }

            jclass callback_class                 = jni_env->GetObjectClass(ctx->callback);
            jmethodID read_characteristics_method = jni_env->GetMethodID(
                callback_class, "onReadCharacteristics", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[B");

            jstring external_device_address = jni_env->NewStringUTF(to_string(deviceId).c_str());
            jstring service_uuid_str        = jni_env->NewStringUTF(winrt::to_string(service_class_w_str).c_str());
            jstring characteristics_uuid_str =
                jni_env->NewStringUTF(winrt::to_string(characteristics_uuid_w_str).c_str());

            const auto read_response = reinterpret_cast<jbyteArray>(
                jni_env->CallObjectMethod(ctx->callback, read_characteristics_method, external_device_address,
                                          service_uuid_str, characteristics_uuid_str));

            jni_env->DeleteLocalRef(service_uuid_str);
            jni_env->DeleteLocalRef(characteristics_uuid_str);
            jni_env->DeleteLocalRef(external_device_address);
            jni_env->DeleteLocalRef(callback_class);

            if (read_response == nullptr) {
                request.RespondWithProtocolError(GattProtocolError::AttributeNotFound());
                WIN_LOG(L"CHARACTERISTIC " << to_hstring(characteristic.Uuid()) << "ATTRIBUTE NOT FOUND");
            } else {
                const jsize len = jni_env->GetArrayLength(read_response);
                jbyte* data     = jni_env->GetByteArrayElements(read_response, nullptr);

                const DataWriter writer;
                writer.WriteBytes(array_view<const uint8_t>(reinterpret_cast<uint8_t*>(data),
                                                            reinterpret_cast<uint8_t*>(data) + len));

                jni_env->ReleaseByteArrayElements(read_response, data, JNI_ABORT);
                request.RespondWithValue(writer.DetachBuffer());

                WIN_LOG(L"READ CHARACTERISTIC " << to_hstring(characteristic.Uuid()) << "DATA SEND SIZE "
                                                << to_hstring(static_cast<uint8_t>(len)));
            }
            deferral.Complete();

            if (thread_ready)
                ctx->vm_ref->DetachCurrentThread();
        });

        // --------------------- CHARACTERISTICS WRITE LISTENER ------------------------------------

        characteristic.WriteRequested(
            [ctx, characteristics_uuid_w_str, service_class_w_str](GattLocalCharacteristic const& characteristic,
                                                                   GattWriteRequestedEventArgs const& args) {
                const auto deferral = args.GetDeferral();
                const auto request  = args.GetRequestAsync().get();
                const auto deviceId = args.Session().DeviceId().Id();
                const auto buffer   = request.Value();

                WIN_LOG(L"WRITE REQUESTED" << to_hstring(characteristic.Uuid()) << L"BUFFER SIZE "
                                           << to_hstring(buffer.Length()));

                JNIEnv* jni_env   = nullptr;
                bool thread_ready = false;

                if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                    ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                    thread_ready = true;
                }

                jclass callback_class = jni_env->GetObjectClass(ctx->callback);
                jmethodID on_write_characteristics_method =
                    jni_env->GetMethodID(callback_class, "onWriteCharacteristicRequest",
                                         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)V");

                jstring device_address   = jni_env->NewStringUTF(to_string(deviceId).c_str());
                jstring service_uuid_str = jni_env->NewStringUTF(winrt::to_string(service_class_w_str).c_str());
                jstring characteristics_uuid_str =
                    jni_env->NewStringUTF(winrt::to_string(characteristics_uuid_w_str).c_str());
                jbyteArray write_request_value = jni_env->NewByteArray(static_cast<jsize>(buffer.Length()));
                jni_env->SetByteArrayRegion(write_request_value, 0, static_cast<jsize>(buffer.Length()),
                                            reinterpret_cast<const jbyte*>(buffer.data()));

                jni_env->CallObjectMethod(ctx->callback, on_write_characteristics_method, device_address,
                                          service_uuid_str, characteristics_uuid_str, write_request_value);

                jni_env->DeleteLocalRef(device_address);
                jni_env->DeleteLocalRef(service_uuid_str);
                jni_env->DeleteLocalRef(characteristics_uuid_str);
                jni_env->DeleteLocalRef(write_request_value);
                jni_env->DeleteLocalRef(callback_class);

                if (request.Option() == GattWriteOption::WriteWithResponse) {
                    WIN_LOG(L"SENDING A WRITE RESPONSE");
                    request.Respond();
                }

                deferral.Complete();

                if (thread_ready)
                    ctx->vm_ref->DetachCurrentThread();
            });

        env->DeleteLocalRef(characteristics_class_method);
        env->DeleteLocalRef(java_characteristics_uuid_ob);
    }

    // --------------------- ADVERTISEMENT_STATUS_LISTENER ------------------------------------
    ctx->service_provider.AdvertisementStatusChanged(
        [ctx](GattServiceProvider const&, GattServiceProviderAdvertisementStatusChangedEventArgs const& args) {
            JNIEnv* jni_env      = nullptr;
            bool is_thread_ready = false;

            if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                is_thread_ready = true;
            }

            jclass callback_class = jni_env->GetObjectClass(ctx->callback);
            jmethodID device_status_change_method =
                jni_env->GetMethodID(callback_class, "onServiceStatusChange", "(I)V");

            jni_env->CallVoidMethod(ctx->callback, device_status_change_method, static_cast<int>(args.Status()));

            if (is_thread_ready)
                ctx->vm_ref->DetachCurrentThread();
        });
}

JNIEXPORT void JNICALL Java_com_sam_blejavaadvertise_BLEAdvertiser_nativeSendNotification(
    JNIEnv* env, jobject, jlong handle, jstring device_address, jstring characteristicsUUID, jbyteArray value,
    jboolean confirm) {

    auto* ctx = reinterpret_cast<NativeContext*>(handle);
    if (!ctx) {
        WIN_LOG("INVALID CONTEXT PROVIDED");
        return;
    }

    auto& characteristics_map           = ctx->characteristics;
    const char* characteristics_str_utf = env->GetStringUTFChars(characteristicsUUID, nullptr);
    hstring characteristics_h_str       = to_hstring(characteristics_str_utf);
    env->ReleaseStringUTFChars(characteristicsUUID, characteristics_str_utf);

    auto found_result = characteristics_map.find(characteristics_h_str.c_str());
    if (found_result == characteristics_map.end()) {
        WIN_LOG(L"CANNOT FIND THE GIVEN CHARACTERISTICS" << characteristics_h_str);
        throw_runtime_exception(env, "Cannot find the given characteristics");
        return;
    }

    const auto characteristics = found_result->second;

    jsize bytes_size = env->GetArrayLength(value);
    jbyte* bytes     = env->GetByteArrayElements(value, nullptr);

    DataWriter writer;
    writer.WriteBytes(std::vector(reinterpret_cast<uint8_t*>(bytes), reinterpret_cast<uint8_t*>(bytes) + bytes_size));
    env->ReleaseByteArrayElements(value, bytes, JNI_ABORT);

    const IBuffer buffer = writer.DetachBuffer();

    const char* device_address_utf = env->GetStringUTFChars(device_address, nullptr);
    std::wstring device_address_w  = to_hstring(device_address_utf).c_str();
    env->ReleaseStringUTFChars(device_address, device_address_utf);

    const auto subscribed_clients = characteristics.SubscribedClients();

    if (subscribed_clients.Size() == 0) {
        WIN_LOG(L"NO SUBSCRIBED CLIENTS FOUND REQUIRED ATLEAST ONE SUBSCRIBER");
        return;
    }

    GattSubscribedClient target_client = nullptr;

    for (auto&& client : subscribed_clients) {
        auto address = client.Session().DeviceId().Id();
        if (address == device_address_w) {
            target_client = client;
            break;
        }
    }

    if (target_client == nullptr) {
        WIN_LOG(L"INVALID DEVICE ADDRESS " << device_address_w << " NO MATCHING ADDRESS FOUND");
        throw_runtime_exception(env, "Invalid device address");
        return;
    }

    try {
        WIN_LOG(L"REQUESTING NOTIFY" << device_address_w << " CHARACTERISTICS " << characteristics_h_str
                                     << " BUFFER SIZE " << buffer.Length() << " IS INDICATION " << confirm);

        const auto operation = characteristics.NotifyValueAsync(buffer, target_client);

        // notification
        if (!confirm)
            return;
        // indication wait for the result

        auto deviceId = target_client.Session().DeviceId().Id();

        operation.Completed([ctx, deviceId, characteristics_h_str](auto const& async_op, AsyncStatus const status) {
            if (status == AsyncStatus::Started || status == AsyncStatus::Canceled) {
                WIN_LOG(L"CHARACTERISTICS NOTIFICATION STATUS STARTED OR CANCELLED");
                return;
            }

            JNIEnv* jni_env   = nullptr;
            bool thread_ready = false;

            if (ctx->vm_ref->GetEnv(reinterpret_cast<void**>(&jni_env), JNI_VERSION_1_6) != JNI_OK) {
                ctx->vm_ref->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), nullptr);
                thread_ready = true;
            }

            jstring device_address_utf         = jni_env->NewStringUTF(to_string(deviceId).c_str());
            jstring characteristics_uuid_utf   = jni_env->NewStringUTF(winrt::to_string(characteristics_h_str).c_str());
            jclass callback_class              = jni_env->GetObjectClass(ctx->callback);
            jmethodID indication_status_method = jni_env->GetMethodID(callback_class, "onIndicationResult",
                                                                      "(Ljava/lang/String;Ljava/lang/String;ZII)V");

            if (status == AsyncStatus::Completed) {
                const GattClientNotificationResult result = async_op.GetResults();

                WIN_LOG(L"CHARACTERISTICS :" << characteristics_h_str << "NOTIFICATION STATUS SUCCESS STATUS :"
                                             << static_cast<int>(result.Status()));

                jni_env->CallVoidMethod(ctx->callback, indication_status_method, device_address_utf,
                                        characteristics_uuid_utf, true, static_cast<int>(result.Status()), -1);
            }
            if (status == AsyncStatus::Error) {
                const hresult hr = async_op.ErrorCode();

                WIN_LOG(L"CHARACTERISTICS :" << characteristics_h_str << "NOTIFICATION STATUS FAILED ERROR CODE "
                                             << hr.value);

                jni_env->CallVoidMethod(ctx->callback, indication_status_method, device_address_utf,
                                        characteristics_uuid_utf, false, -1, static_cast<int>(hr.value));
            }

            if (jni_env->ExceptionCheck()) {
                jni_env->ExceptionDescribe();
                jni_env->ExceptionClear();
            }

            jni_env->DeleteLocalRef(callback_class);
            jni_env->DeleteLocalRef(device_address_utf);
            jni_env->DeleteLocalRef(characteristics_uuid_utf);

            if (thread_ready)
                ctx->vm_ref->DetachCurrentThread();
        });

    } catch (hresult_error const& ex) {
        const auto message = "WinRT Exception: " + to_string(ex.message());
        throw_runtime_exception(env, message.c_str());
    }
}
}
