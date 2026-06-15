package com.sam.bt_common

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
interface BTInfoProvider {

    fun registerCallback(callback: (Boolean) -> Unit): Long
    fun unregisterCallback(caller: Long)

    suspend fun isBluetoothActive(): Boolean
    fun isLEConnectionAllowed(): Boolean
    fun isPeripheralRoleSupported(): Boolean
}
