package com.sam.bluepad.data.ble.exceptions

import android.bluetooth.BluetoothGatt

class GattInvalidStatusException(val status: Int) : Exception("GATT Status Failed") {

    val statusMessage: String
        get() = when (status) {
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "Characteristics read are not allowed"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Characteristics write are not allowed"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION -> "Authentication/Authorization missing"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "Invalid request type"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "Encryption missing"
            BluetoothGatt.GATT_INVALID_OFFSET -> "Invalid internal code"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "MTU IS SHORT AS PER THE GIVEN PAYLOAD"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "Congested connection"
            BluetoothGatt.GATT_CONNECTION_TIMEOUT -> "Connection timeout"
            BluetoothGatt.GATT_FAILURE -> "Bluetooth gatt connection failed"
            else -> "Some invalid state"
        }
}