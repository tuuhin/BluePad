package com.sam.ble_advertise

internal object BLEAdvertiserCallbackDefaults {

    fun empty(): BLEAdvertiserCallback = object : BLEAdvertiserCallback {
        override fun onServiceAdded(serviceUuid: String, success: Boolean, errorCode: Int) {}
        override fun onServiceStatusChanged(status: Int) {}
        override fun onReadCharacteristics(
            deviceAddress: String,
            serviceUuid: String,
            characteristicUuid: String,
            status: Int
        ): String = ""

        override fun onWriteCharacteristics(
            deviceAddress: String,
            serviceUuid: String,
            characteristicUuid: String,
            value: ByteArray,
            respond: Boolean
        ): Int = 0

        override fun onReadDescriptor(
            deviceAddress: String,
            serviceUuid: String,
            characteristicUuid: String,
            descriptorUuid: String,
            status: Int
        ): String = ""

        override fun onWriteDescriptor(
            deviceAddress: String,
            serviceUuid: String,
            characteristicUuid: String,
            descriptorUuid: String,
            value: ByteArray,
            respond: Boolean
        ): Int = 0

        override fun onIndicationResult(
            deviceAddress: String,
            characteristicUuid: String,
            success: Boolean,
            status: Int,
            errorCode: Int
        ) {
        }
    }


    fun create(
        onServiceAdded: OnServiceAdded,
        onServiceStatusChanged: OnServiceChanged,
        onReadCharacteristics: OnReadCharacteristics,
        onWriteCharacteristics: OnWriteCharacteristics,
        onReadDescriptor: OnReadDescriptor,
        onWriteDescriptor: OnWriteDescriptor,
        onIndicationResult: OnIndicationResult
    ): BLEAdvertiserCallback {
        return object : BLEAdvertiserCallback {

            override fun onServiceAdded(serviceUuid: String, success: Boolean, errorCode: Int) =
                onServiceAdded(serviceUuid, success, errorCode)

            override fun onServiceStatusChanged(status: Int) = onServiceStatusChanged(status)

            override fun onReadCharacteristics(
                deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int
            ): String = onReadCharacteristics(deviceAddress, serviceUuid, characteristicUuid, status)

            override fun onWriteCharacteristics(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                value: ByteArray,
                respond: Boolean
            ): Int = onWriteCharacteristics(deviceAddress, serviceUuid, characteristicUuid, value, respond)

            override fun onReadDescriptor(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                descriptorUuid: String,
                status: Int
            ): String = onReadDescriptor(deviceAddress, serviceUuid, characteristicUuid, descriptorUuid, status)

            override fun onWriteDescriptor(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                descriptorUuid: String,
                value: ByteArray,
                respond: Boolean
            ): Int = onWriteDescriptor(
                deviceAddress,
                serviceUuid,
                characteristicUuid,
                descriptorUuid,
                value,
                respond,
            )

            override fun onIndicationResult(
                deviceAddress: String,
                characteristicUuid: String,
                success: Boolean,
                status: Int,
                errorCode: Int
            ) = onIndicationResult(deviceAddress, characteristicUuid, success, status, errorCode)
        }
    }
}
