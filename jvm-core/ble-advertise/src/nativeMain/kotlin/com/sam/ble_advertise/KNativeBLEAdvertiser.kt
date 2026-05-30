package com.sam.ble_advertise

import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.BLECharacteristicsModel
import com.sam.ble_advertise.models.GATTAdvertiseConfig

interface KNativeBLEAdvertiser {

    fun getStatus(): BLEAdvertisementStatus

    fun start(config: GATTAdvertiseConfig)

    fun stop()

    fun addService(serviceUuid: String)

    fun addCharacteristic(characteristic: BLECharacteristicsModel)

    fun addDescriptor(characteristicUuid: String, descriptorUuid: String)

    fun sendNotification(deviceAddress: String, characteristicUuid: String, value: ByteArray)

    fun registerForCallbacks(
        onServiceAdded: (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit,
        onServiceStatusChanged: (status: BLEAdvertisementStatus) -> Unit,
        onReadCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String,
        onWriteCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onReadDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String,
        onWriteDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onIndicationResult: (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
    )

    fun onDestroy()
}
