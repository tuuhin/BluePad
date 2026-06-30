package com.sam.bluepad.domain.exceptions

class BluetoothInvalidBondRequest(val address: String) :
    Exception("Bluetooth device: $address is already bonded to the system")
