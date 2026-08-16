package com.sam.bluepad.device_info

import co.touchlab.kermit.Logger
import platform.IOBluetooth.IOBluetoothHostController

actual class PlatformDeviceBluetoothInfo : IDeviceBluetoothInfo {

    private var _cachedAdapterName: String? = null
    private var _cachedManufactureName: String? = null
    private var _cachedMacAddress: String? = null
    private var _cachedVersion: String? = null

    actual override val adapterName: String?
        get() {
            if (_cachedAdapterName == null) {
                _cachedAdapterName = hostController { controller -> controller.nameAsString() }
            }
            return _cachedAdapterName
        }

    actual override val manufacture: String?
        get() = null

    actual override val macAddress: String?
        get() {
            if (_cachedMacAddress == null) {
                val macAddress = hostController { controller -> controller.addressAsString() }
                _cachedMacAddress = macAddress
            }
            return _cachedMacAddress
        }

    actual override val bluetoothVersion: String?
        get() = null

    private inline fun <T> hostController(operate: (IOBluetoothHostController) -> T): T? {
        val controller = IOBluetoothHostController.defaultController()
            ?: run {
                _logger.w { "FAILED TO READ BLUETOOTH HOST CONTROLLER" }
                return null
            }
        return operate(controller)
    }

    companion object {
        private val _logger = Logger.withTag("DEVICE_INFO_READER")
    }

}
