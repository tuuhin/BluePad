package com.sam.ble_advertise


internal interface BLEAdvertiserCallback {
    fun onServiceAdded(serviceUuid: String, success: Boolean, errorCode: Int)

    fun onServiceStatusChanged(status: Int)

    fun onReadCharacteristics(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        status: Int
    ): String

    fun onWriteCharacteristics(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        value: ByteArray,
        respond: Boolean
    ): Int

    fun onReadDescriptor(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        descriptorUuid: String,
        status: Int
    ): String

    fun onWriteDescriptor(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        descriptorUuid: String,
        value: ByteArray,
        respond: Boolean
    ): Int

    fun onIndicationResult(
        deviceAddress: String,
        characteristicUuid: String,
        success: Boolean,
        status: Int,
        errorCode: Int
    )

}
