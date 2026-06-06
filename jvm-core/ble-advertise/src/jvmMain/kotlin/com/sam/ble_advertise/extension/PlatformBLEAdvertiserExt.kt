package com.sam.ble_advertise.extension

import com.sam.ble_advertise.BLEAdvertiserListener
import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.Service
import com.sam.ble_advertise.platform.BLECharacteristicsModel
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.uuid.Uuid

fun PlatformBLEAdvertiser.getStatus(): BLEAdvertisementStatus =
    BLEAdvertisementStatus.bLEAdvertisementStatusFromInt(getStatusInt())

fun PlatformBLEAdvertiser.addService(service: Service) {
    addService(service.uuid.toHexDashString())
    service.characteristics.forEach { characteristic ->
        val characteristics = BLECharacteristicsModel(
            characteristicUuid = characteristic.uuid.toHexDashString(),
            canRead = characteristic.canRead,
            canIndicate = characteristic.canIndicate,
            canNotify = characteristic.canNotify,
            canWrite = characteristic.canWriteRequest,
            canWriteNoResponse = characteristic.canWriteCommand,
        )
        addCharacteristic(characteristics)
        characteristic.descriptors.forEach { desc ->
            addDescriptor(
                characteristicUuid = characteristic.uuid.toHexDashString(),
                descriptorUuid = desc.uuid.toHexDashString(),
            )
        }
    }
}


fun PlatformBLEAdvertiser.setListener(listener: BLEAdvertiserListener, scope: CoroutineScope) = registerForCallbacks(
    onServiceAdded = { serviceId, isSuccess, errorCode ->
        val uuid = Uuid.parse(serviceId)
        scope.launch { listener.onServiceAdded(uuid, isSuccess, errorCode) }
    },
    onServiceStatusChanged = { status ->
        scope.launch { listener.onServiceStatusChange(BLEAdvertisementStatus.bLEAdvertisementStatusFromInt(status)) }
    },
    onReadCharacteristics = { deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int ->
        runBlocking(scope.coroutineContext) {
            val serviceId = Uuid.parse(serviceUuid)
            val characteristicsId = Uuid.parse(characteristicUuid)
            val bytes =
                listener.onReadCharacteristic(deviceAddress, serviceId, characteristicsId) ?: byteArrayOf()
            bytes.decodeToString()
        }
    },
    onWriteCharacteristics = { deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean ->
        runBlocking(scope.coroutineContext) {
            val serviceId = Uuid.parse(serviceUuid)
            val characteristicsId = Uuid.parse(characteristicUuid)
            val response =
                listener.onWriteCharacteristic(deviceAddress, serviceId, characteristicsId, value)
            response.code
        }
    },
    onReadDescriptor = { deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorId: String, status: Int ->
        runBlocking(scope.coroutineContext) {
            val serviceId = Uuid.parse(serviceUuid)
            val characteristicsId = Uuid.parse(characteristicUuid)
            val descriptorId = Uuid.parse(descriptorId)
            val bytes = listener.onReadDescriptor(deviceAddress, serviceId, characteristicsId, descriptorId, status)
                ?: byteArrayOf()
            bytes.decodeToString()
        }
    },
    onWriteDescriptor = { deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean ->
        runBlocking(scope.coroutineContext) {
            val serviceId = Uuid.parse(serviceUuid)
            val characteristicsId = Uuid.parse(characteristicUuid)
            val descriptorId = Uuid.parse(descriptorUuid)
            val response =
                listener.onWriteDescriptor(deviceAddress, serviceId, characteristicsId, descriptorId, value)
            response.code
        }
    },
    onIndicationResult = { deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int ->
        scope.launch {
            val characteristicsId = Uuid.parse(characteristicUuid)
            listener.onIndicationResult(deviceAddress, characteristicsId, success, status, errorCode)
        }
    },
)

