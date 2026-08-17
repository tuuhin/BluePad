package com.sam.bluepad.device_info

expect class PlatformDeviceBluetoothInfo : IDeviceBluetoothInfo {
    override val adapterName: String?
    override val manufacture: String?
    override val macAddress: String?
}
