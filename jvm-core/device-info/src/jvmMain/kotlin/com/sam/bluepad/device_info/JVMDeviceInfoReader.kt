package com.sam.bluepad.com.sam.bluepad.device_info

import com.sam.bluepad.com.sam.bluepad.device_info.models.JvmDeviceBTAdapterInfo
import com.sam.bluepad.platform.device_info.PlatformDeviceBluetoothInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JVMDeviceInfoReader : IJvmDeviceInfoReader {

    override suspend fun readDevice(): JvmDeviceBTAdapterInfo {
        val provider = PlatformDeviceBluetoothInfo()
        return withContext(Dispatchers.IO) {
            provider.use { provider ->
                JvmDeviceBTAdapterInfo(
                    bluetoothAdapter = provider.adapterName,
                    bluetoothVendor = provider.manufacture,
                    macAddress = provider.macAddress,
                )
            }
        }
    }
}
