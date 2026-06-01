package com.sam.ble_advertise

import com.sam.ble_advertise.models.BLECharacteristicsModel
import com.sam.ble_advertise.models.GATTAdvertiseConfig
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
expect class PlatformBLEAdvertiser : KNativeBLEAdvertiser {

    override fun getStatusInt(): Int

    override fun start(config: GATTAdvertiseConfig)
    override fun stop()

    override fun addService(serviceUuid: String)
    override fun addCharacteristic(characteristic: BLECharacteristicsModel)
    override fun addDescriptor(characteristicUuid: String, descriptorUuid: String)

    override fun sendNotification(deviceAddress: String, characteristicUuid: String, value: ByteArray)

    override fun registerForCallbacks(
        onServiceAdded: (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit,
        onServiceStatusChanged: (status: Int) -> Unit,
        onReadCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String,
        onWriteCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onReadDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String,
        onWriteDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onIndicationResult: (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
    )

    override fun onDestroy()
}
