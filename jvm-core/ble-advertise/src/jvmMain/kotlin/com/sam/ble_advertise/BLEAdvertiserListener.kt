package com.sam.ble_advertise

import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.GattWriteResponse
import kotlin.uuid.Uuid

interface BLEAdvertiserListener {

    suspend fun onServiceAdded(serviceUuid: Uuid, success: Boolean, errorCode: Int)

    suspend fun onServiceStatusChange(status: BLEAdvertisementStatus)

    suspend fun onReadCharacteristic(deviceAddress: String, serviceUuid: Uuid, characteristicUuid: Uuid): ByteArray?

    suspend fun onWriteCharacteristic(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        value: ByteArray,
    ): GattWriteResponse

    suspend fun onReadDescriptor(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        descriptorUuid: Uuid,
        status: Int,
    ): ByteArray?


    suspend fun onWriteDescriptor(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        descriptorUuid: Uuid,
        value: ByteArray,
    ): GattWriteResponse

    suspend fun onIndicationResult(
        address: String,
        characteristicUuid: Uuid,
        success: Boolean,
        status: Int,
        errorCode: Int
    )
}
