package com.sam.bluepad.device_info

import co.touchlab.kermit.Logger
import com.sam.bluepad.windows.bluetooth.BLUETOOTH_FIND_RADIO_PARAMS
import com.sam.bluepad.windows.bluetooth.BLUETOOTH_RADIO_INFO
import com.sam.bluepad.windows.bluetooth.BluetoothFindFirstRadio
import com.sam.bluepad.windows.bluetooth.BluetoothFindNextRadio
import com.sam.bluepad.windows.bluetooth.BluetoothFindRadioClose
import com.sam.bluepad.windows.bluetooth.BluetoothGetRadioInfo
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.windows.CloseHandle
import platform.windows.ERROR_NO_MORE_ITEMS
import platform.windows.ERROR_SUCCESS
import platform.windows.GetLastError
import platform.windows.HANDLEVar

actual class PlatformDeviceBluetoothInfo : IDeviceBluetoothInfo {

    private var _cachedAdapterName: String? = null
    private var _cachedManufactureName: String? = null
    private var _cachedMacAddress: String? = null
    private var _cachedVersion: String? = null

    actual override val adapterName: String?
        get() {
            if (_cachedAdapterName == null) {
                _cachedAdapterName = readRadio { radio -> radio.szName.toKString() }
            }
            return _cachedAdapterName
        }

    actual override val manufacture: String?
        get() {
            if (_cachedManufactureName == null) {
                val code = readRadio { radio -> radio.manufacturer.toInt() } ?: return null
                _cachedManufactureName = DeviceCommons.manufactureMap.getOrElse(code) { "Unknown manufacturer" }
            }
            return _cachedManufactureName
        }

    actual override val macAddress: String?
        get() {
            if (_cachedMacAddress == null) {
                val macAddress = readRadio { radio ->
                    buildString {
                        val bytes = radio.address.rgBytes
                        for (i in 5 downTo 0) {
                            val hexByte = bytes[i].toString(radix = 16)
                                .padStart(2, '0')
                                .uppercase()
                            append(hexByte)
                            if (i > 0) append(":")
                        }
                    }
                }
                _cachedMacAddress = macAddress
            }
            return _cachedMacAddress
        }

    actual override val bluetoothVersion: String?
        get() {
            if (_cachedVersion == null) {
                val version = readRadio { radio -> radio.lmpSubversion }?.toInt() ?: return null
                _cachedVersion = DeviceCommons.lmpVersions.getOrElse(version) { "UNKNOWN VERSION" }
            }
            return _cachedVersion
        }


    private inline fun <T> readRadio(operate: (BLUETOOTH_RADIO_INFO) -> T): T? = memScoped {
        val findParams = alloc<BLUETOOTH_FIND_RADIO_PARAMS>()
            .apply {
                dwSize = sizeOf<BLUETOOTH_FIND_RADIO_PARAMS>().toUInt()
            }

        val radioInfo = alloc<BLUETOOTH_RADIO_INFO>().apply {
            dwSize = sizeOf<BLUETOOTH_RADIO_INFO>().toUInt()
        }
        val hRadioPtr = alloc<HANDLEVar>()

        val hFind = BluetoothFindFirstRadio(findParams.ptr, hRadioPtr.ptr)
        if (hFind == null) {
            val err = GetLastError()
            if (err == ERROR_NO_MORE_ITEMS.toUInt()) _logger.w { "NO BLUETOOTH ADAPTERS FOUND " }
            else _logger.w { "BLUETOOTH RADIO FIND FAILED ERROR CODE: $err" }
            return null
        }
        try {
            do {
                val hRadio = hRadioPtr.value
                if (BluetoothGetRadioInfo(hRadio, radioInfo.ptr) == ERROR_SUCCESS.toUInt()) {
                    return@memScoped operate(radioInfo)
                }
                CloseHandle(hRadio)
            } while (BluetoothFindNextRadio(hFind, hRadioPtr.ptr) != 0)
        } finally {
            BluetoothFindRadioClose(hFind)
        }
        return@memScoped null
    }

    companion object {
        private val _logger = Logger.withTag("DEVICE_INFO_READER")
    }

}
