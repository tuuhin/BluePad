package com.sam.ble_advertise

import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.GattWriteResponse
import kotlin.uuid.Uuid

internal object BLEAdvertiseTestListener : BLEAdvertiserListener {

    override suspend fun onServiceAdded(serviceUuid: Uuid, success: Boolean, errorCode: Int) = Unit

    override suspend fun onServiceStatusChange(status: BLEAdvertisementStatus) = Unit

    override suspend fun onReadCharacteristic(deviceAddress: String, serviceUuid: Uuid, characteristicUuid: Uuid)
        : ByteArray = byteArrayOf()

    override suspend fun onWriteCharacteristic(
        address: String, serviceUuid: Uuid, characteristicUuid: Uuid, value: ByteArray
    ): GattWriteResponse = GattWriteResponse.SUCCESS

    override suspend fun onReadDescriptor(
        address: String, serviceUuid: Uuid,
        characteristicUuid: Uuid, descriptorUuid: Uuid, status: Int
    ): ByteArray = byteArrayOf()

    override suspend fun onWriteDescriptor(
        address: String, serviceUuid: Uuid, characteristicUuid: Uuid,
        descriptorUuid: Uuid, value: ByteArray
    ): GattWriteResponse = GattWriteResponse.SUCCESS

    override suspend fun onIndicationResult(
        address: String,
        characteristicUuid: Uuid,
        success: Boolean,
        status: Int,
        errorCode: Int
    ) = Unit
}
