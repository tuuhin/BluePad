package com.sam.bt_common

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.sam.bt_common.platform.mingw.BluetoothStatusCallback
import com.sam.bt_common.platform.mingw.ble_is_bluetooth_active
import com.sam.bt_common.platform.mingw.ble_is_peripheral_role_supported
import com.sam.bt_common.platform.mingw.ble_is_secure_connection_available
import com.sam.bt_common.platform.mingw.bluetooth_caller_register_listener
import com.sam.bt_common.platform.mingw.bluetooth_caller_unregister_listener
import com.sam.bt_common.platform.mingw.init_logger
import com.sam.bt_common.platform.mingw.request_bluetooth_enable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import platform.windows.SW_SHOWNORMAL
import platform.windows.ShellExecuteW


@OptIn(ExperimentalForeignApi::class)
actual class PlatformBTInfoProvider : BTInfoProvider {

    init {
        // initialize the cpp logger and ansi colors
        init_logger()
        enableWindowsAnsiColors()
    }

    actual override fun registerCallback(callback: (Boolean) -> Unit): Long {
        _callbackRef = callback
        val callerPointer = bluetooth_caller_register_listener(_statusCallback)
        return callerPointer.toLong()
    }

    actual override fun unregisterCallback(caller: Long) {
        bluetooth_caller_unregister_listener(caller.toCPointer())
        _callbackRef = null
    }

    actual override fun requestBTEnable(): Int {
        return request_bluetooth_enable()
    }

    actual override fun openBTSettings() = memScoped {
        val result = ShellExecuteW(
            null,
            "open",
            "ms-settings:bluetooth",
            null,
            null,
            SW_SHOWNORMAL,
        )
        // ShellExecute returns a value greater than 32 on success
        if (result.toLong() > 32) {
            logger.i { "SETTINGS OPEN SUCCESSFULLY" }
        } else {
            logger.e { "FAILED TO LAUNCH SETTINGS Error code: ${result.toLong()}" }
        }
    }

    actual override val canActivateBTFromApp: Boolean = true
    actual override val canRequestOpenSettings: Boolean = true

    actual override suspend fun isBluetoothActive(): Boolean = ble_is_bluetooth_active()


    actual override fun isLEConnectionAllowed(): Boolean = ble_is_secure_connection_available()
    actual override fun isPeripheralRoleSupported(): Boolean = ble_is_peripheral_role_supported()

    companion object {
        private var _callbackRef: ((Boolean) -> Unit)? = null

        // This function is purely static and captures absolutely nothing
        private val _statusCallback: BluetoothStatusCallback = staticCFunction { isOn: Boolean ->
            _callbackRef?.invoke(isOn)
        }

        private val logger = Logger(
            loggerConfigInit(logWriters = arrayOf(WindowsLogWriter(enabled = false))),
            "WIN",
        )
    }
}
