package com.sam.bluepad.device_info

interface IDeviceBluetoothInfo {
    val adapterName: String?
    val manufacture: String?
    val macAddress: String?
    val bluetoothVersion: String?
}
