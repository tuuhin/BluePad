package com.sam.bluepad.bluetooth.exceptions

internal class BluetoothInvalidBondRequest(
    val address: String
) : Exception("Bluetooth device: $address is already bonded to the system")
