package com.sam.bluepad.device_info

import co.touchlab.kermit.Logger
import platform.IOBluetooth.IOBluetoothHostController

actual class PlatformDeviceBluetoothInfo : IDeviceBluetoothInfo {

    private var _cachedAdapterName: String? = null
    private var _cachedMacAddress: String? = null

    actual override val adapterName: String?
        get() {
            if (_cachedAdapterName == null) {
                _cachedAdapterName = hostController { controller -> controller.nameAsString() }
            }
            return _cachedAdapterName
        }

    actual override val manufacture: String?
        // Unable to read bluetooth adapter manufacturer
        get() = null

    actual override val macAddress: String?
        get() {
            if (_cachedMacAddress == null) {
                _cachedMacAddress = hostController { controller -> controller.addressAsString() }
            }
            return _cachedMacAddress
        }


    private inline fun <T> hostController(operate: (IOBluetoothHostController) -> T): T? {
        val controller = IOBluetoothHostController.defaultController()
        if (controller == null) {
            _logger.w { "FAILED TO READ BLUETOOTH HOST CONTROLLER" }
            return null
        }
        return operate(controller)
    }

    companion object {
        private val _logger = Logger.withTag("DEVICE_INFO_READER")
    }

}
