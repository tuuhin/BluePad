package com.sam.bt_common

import com.sam.bt_common.platform.mingw.BluetoothStatusCallback
import com.sam.bt_common.platform.mingw.ble_is_bluetooth_active
import com.sam.bt_common.platform.mingw.ble_is_peripheral_role_supported
import com.sam.bt_common.platform.mingw.ble_is_secure_connection_available
import com.sam.bt_common.platform.mingw.bluetooth_caller_register_listener
import com.sam.bt_common.platform.mingw.bluetooth_caller_unregister_listener
import com.sam.bt_common.platform.mingw.init_logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong

@OptIn(ExperimentalForeignApi::class)
actual class PlatformBTInfoProvider : BTInfoProvider {

    init {
        // initialize the cpp logger
        init_logger()
    }

    actual override fun registerCallback(callback: (Boolean) -> Unit): Long {
        globalCallback = callback
        val callerPointer = bluetooth_caller_register_listener(staticCallback)
        return callerPointer.toLong()
    }

    actual override fun unregisterCallback(caller: Long) {
        bluetooth_caller_unregister_listener(caller.toCPointer())
        globalCallback = null
    }

    actual override fun isBluetoothActive(): Boolean = ble_is_bluetooth_active()
    actual override fun isLEConnectionAllowed(): Boolean = ble_is_secure_connection_available()
    actual override fun isPeripheralRoleSupported(): Boolean = ble_is_peripheral_role_supported()

    companion object {
        private var globalCallback: ((Boolean) -> Unit)? = null

        // This function is purely static and captures absolutely nothing
        val staticCallback: BluetoothStatusCallback = staticCFunction { isOn: Boolean ->
            globalCallback?.invoke(isOn)
        }
    }
}
