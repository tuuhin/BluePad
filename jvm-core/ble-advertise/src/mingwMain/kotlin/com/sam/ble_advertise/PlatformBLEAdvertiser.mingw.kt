@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.ble_advertise

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.sam.ble_advertise.models.BLECharacteristicsModel
import com.sam.ble_advertise.models.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.mingw.BLEAdvertiserPtr
import com.sam.ble_advertise.platform.mingw.ble_advertiser_add_characteristic
import com.sam.ble_advertise.platform.mingw.ble_advertiser_add_descriptor
import com.sam.ble_advertise.platform.mingw.ble_advertiser_add_service
import com.sam.ble_advertise.platform.mingw.ble_advertiser_create
import com.sam.ble_advertise.platform.mingw.ble_advertiser_destroy
import com.sam.ble_advertise.platform.mingw.ble_advertiser_get_status
import com.sam.ble_advertise.platform.mingw.ble_advertiser_register_callbacks
import com.sam.ble_advertise.platform.mingw.ble_advertiser_respond_read
import com.sam.ble_advertise.platform.mingw.ble_advertiser_respond_write
import com.sam.ble_advertise.platform.mingw.ble_advertiser_send_notification
import com.sam.ble_advertise.platform.mingw.ble_advertiser_start
import com.sam.ble_advertise.platform.mingw.ble_advertiser_stop
import com.sam.ble_advertise.platform.mingw.ble_characteristics
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toLong
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch

private typealias MingwBLEAdvertiserConfig = com.sam.ble_advertise.platform.mingw.BLEAdvertiseConfig
private typealias MingwBLEAdvertiserCallbacks = com.sam.ble_advertise.platform.mingw.BLEAdvertiserCallbacks

private class MingwCallbackContainer(
    val onServiceAdded: (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit,
    val onStatusChanged: (status: Int) -> Unit,
    val onReadCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String?,
    val onWriteCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int,
    val onReadDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String?,
    val onWriteDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int,
    val onIndicationResult: (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
)

actual class PlatformBLEAdvertiser : KNativeBLEAdvertiser {

    init {
        enableWindowsAnsiColors()
    }

    actual override fun addService(serviceUuid: String) {
        val handle = createHandleIfNotSet()
        ble_advertiser_add_service(advertiser = handle, service_uuid = serviceUuid)
    }

    actual override fun addCharacteristic(characteristic: BLECharacteristicsModel) = memScoped {
        val handle = createHandleIfNotSet()

        val mingWCharacteristics = alloc<ble_characteristics>()
            .apply {
                characteristic_uuid = characteristic.characteristicUuid.cstr.getPointer(this@memScoped)
                can_read = characteristic.canRead
                can_write = characteristic.canWrite
                can_notify = characteristic.canNotify
                can_write_no_response = characteristic.canWriteNoResponse
                can_indicate = characteristic.canIndicate
            }

        ble_advertiser_add_characteristic(
            advertiser = handle,
            characteristics = mingWCharacteristics.readValue(),
        )
    }

    actual override fun addDescriptor(characteristicUuid: String, descriptorUuid: String) {
        val handle = createHandleIfNotSet()
        ble_advertiser_add_descriptor(
            advertiser = handle,
            characteristic_uuid = characteristicUuid,
            descriptor_uuid = descriptorUuid,
        )
    }

    actual override fun registerForCallbacks(
        onServiceAdded: (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit,
        onServiceStatusChanged: (status: Int) -> Unit,
        onReadCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String,
        onWriteCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onReadDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String,
        onWriteDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onIndicationResult: (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
    ) {
        if (_callbackRef != null) {
            _callbackRef?.dispose()
            logger.w { "OLD CALLBACK REMOVED" }
        }

        val container = MingwCallbackContainer(
            onServiceAdded = onServiceAdded,
            onStatusChanged = onServiceStatusChanged,
            onReadCharacteristics = onReadCharacteristics,
            onWriteCharacteristics = onWriteCharacteristics,
            onReadDescriptor = onReadDescriptor,
            onWriteDescriptor = onWriteDescriptor,
            onIndicationResult = onIndicationResult,
        )

        val stableRef = StableRef.create(container)
        _callbackRef = stableRef

        memScoped {
            val callbacks = alloc<MingwBLEAdvertiserCallbacks>().apply {
                this.user_data = stableRef.asCPointer()
                this.on_service_added = staticCFunction { serviceUuid, errorCode, userData ->
                    val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction
                    val success = errorCode == 0
                    ref.onServiceAdded(serviceUuid?.toKString() ?: "", success, errorCode)
                }

                this.on_service_status_change = staticCFunction { status, userData ->
                    val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction
                    ref.onStatusChanged(status)
                }

                this.on_read_characteristic =
                    staticCFunction { request, address, service, characteristic, status, userData ->
                        val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction

                        val bytes = ref.onReadCharacteristics(
                            address?.toKString() ?: "",
                            service?.toKString() ?: "",
                            characteristic?.toKString() ?: "",
                            status,
                        )?.encodeToByteArray() ?: byteArrayOf()

                        if (bytes.isEmpty()) {
                            ble_advertiser_respond_read(request, null, 0u, 1)
                            return@staticCFunction
                        }

                        bytes.usePinned { pinned ->
                            ble_advertiser_respond_read(
                                request,
                                pinned.addressOf(0).reinterpret(),
                                bytes.size.toULong(),
                                0,
                            )
                        }
                    }


                this.on_write_characteristic =
                    staticCFunction { request, deviceAddress, serviceUuid, characteristicUuid, data, len, respondNeeded, userData ->
                        val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction

                        val ktArray = ByteArray(len.toInt())
                        if (len > 0u && data != null) {
                            ktArray.usePinned { pinned ->
                                memcpy(pinned.addressOf(0), data, len)
                            }
                        }

                        val response = ref.onWriteCharacteristics(
                            deviceAddress?.toKString() ?: "",
                            serviceUuid?.toKString() ?: "",
                            characteristicUuid?.toKString() ?: "",
                            ktArray,
                            respondNeeded,
                        )
                        if (respondNeeded) ble_advertiser_respond_write(request, response)
                    }

                this.on_read_descriptor =
                    staticCFunction { request, deviceAddress, serviceUuid, characteristicUuid, descriptorUuid, status, userData ->
                        val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction

                        val bytes = ref.onReadDescriptor(
                            deviceAddress?.toKString() ?: "",
                            serviceUuid?.toKString() ?: "",
                            characteristicUuid?.toKString() ?: "",
                            descriptorUuid?.toKString() ?: "",
                            status,
                        )?.encodeToByteArray() ?: byteArrayOf()

                        if (bytes.isEmpty()) {
                            ble_advertiser_respond_read(request, null, 0u, 1)
                            return@staticCFunction
                        }

                        bytes.usePinned { pinned ->
                            ble_advertiser_respond_read(
                                request,
                                pinned.addressOf(0).reinterpret(),
                                bytes.size.toULong(),
                                0,
                            )
                        }
                    }

                this.on_write_descriptor =
                    staticCFunction { request, deviceAddress, serviceUuid, characteristicUuid, descriptorUuid, data, len, respondNeeded, userData ->
                        val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction

                        val ktArray = ByteArray(len.toInt())
                        if (len > 0u && data != null) {
                            ktArray.usePinned { pinned ->
                                memcpy(pinned.addressOf(0), data, len)
                            }
                        }

                        val response = ref.onWriteDescriptor(
                            deviceAddress?.toKString() ?: "",
                            serviceUuid?.toKString() ?: "",
                            characteristicUuid?.toKString() ?: "",
                            descriptorUuid?.toKString() ?: "",
                            ktArray,
                            respondNeeded,
                        )
                        if (respondNeeded) ble_advertiser_respond_write(request, response)
                    }

                this.on_indication_result =
                    staticCFunction { deviceAddress, characteristicUuid, success, status, errorCode, userData ->
                        val ref = userData?.asStableRef<MingwCallbackContainer>()?.get() ?: return@staticCFunction
                        ref.onIndicationResult(
                            deviceAddress?.toKString() ?: "",
                            characteristicUuid?.toKString() ?: "",
                            success,
                            status,
                            errorCode,
                        )
                    }
            }
            val handle = createHandleIfNotSet()
            ble_advertiser_register_callbacks(handle, callbacks.readValue())
            logger.d { "BLE CALLBACKS REGISTERED" }
        }
    }

    actual override fun getStatusInt(): Int {
        val handle = createHandleIfNotSet()
        return ble_advertiser_get_status(advertiser = handle)
    }


    actual override fun sendNotification(
        deviceAddress: String,
        characteristicUuid: String,
        value: ByteArray
    ): Boolean {
        val handle = createHandleIfNotSet()
        if (value.isEmpty()) {
            return ble_advertiser_send_notification(
                advertiser = handle,
                device_address = deviceAddress,
                characteristic_uuid = characteristicUuid,
                value = null,
                value_len = 0u,
            )
        }
        return value.usePinned { pinned ->
            val bytePtr: CPointer<ByteVar> = pinned.addressOf(0)
            val uBytePtr = bytePtr.reinterpret<UByteVar>()
            ble_advertiser_send_notification(
                advertiser = handle,
                device_address = deviceAddress,
                characteristic_uuid = characteristicUuid,
                value = uBytePtr,
                value_len = value.size.toULong(),
            )
        }
    }


    actual override fun start(config: GATTAdvertiseConfig) = memScoped {
        val handle = createHandleIfNotSet()

        val cConfig = alloc<MingwBLEAdvertiserConfig>()
        cConfig.connectable = config.connectable
        cConfig.discoverable = config.discoverable

        if (config.serviceData.isNotEmpty()) {
            val dataAsBytes = config.serviceData.encodeToByteArray()
            dataAsBytes.usePinned { pinned ->
                val bytePtr: CPointer<ByteVar> = pinned.addressOf(0)
                val uBytePtr = bytePtr.reinterpret<UByteVar>()
                cConfig.service_data = uBytePtr
                cConfig.service_data_len = dataAsBytes.size.toULong()
                ble_advertiser_start(advertiser = handle, config = cConfig.readValue())
            }
            logger.d { "BLE REF STARTED WITH SERVICE DATA" }
        } else {
            cConfig.service_data = null
            cConfig.service_data_len = 0u
            ble_advertiser_start(advertiser = handle, config = cConfig.readValue())
            logger.d { "BLE REF STARTED WITH NO SERVICE DATA" }
        }
    }

    actual override fun stop() {
        val handle = readCurrentHandle() ?: return
        ble_advertiser_stop(handle)
    }

    actual override fun onDestroy() {
        _callbackRef?.dispose()
        _callbackRef = null
        logger.d { "CALLBACK REF REMOVED" }

        val handle = readCurrentHandle()
        if (handle != null) {
            logger.d { "DESTROYING ADVERTISER INSTANCE" }
            _handle.store(-1L)
            ble_advertiser_destroy(handle)
        }
    }

    private fun createHandleIfNotSet(): BLEAdvertiserPtr? {
        val handleValue = _handle.load()
        if (handleValue != -1L) return handleValue.toCPointer()
        val pointer = ble_advertiser_create() ?: return null
        logger.d { "NEW ADVERTISER INSTANCE CREATED" }
        _handle.updateAndFetch { pointer.toLong() }
        return pointer
    }

    private fun readCurrentHandle(): BLEAdvertiserPtr? {
        val handleValue = _handle.load()
        return if (handleValue != -1L) handleValue.toCPointer() else null
    }

    companion object {
        private var _callbackRef: StableRef<MingwCallbackContainer>? = null
        private val _handle = AtomicLong(-1L)

        // disable the logger for now
        private val logger = Logger(
            loggerConfigInit(logWriters = arrayOf(WindowsLogWriter(enabled = false))),
            "WIN",
        )
    }
}
