package com.sam.bt_common

expect class PlatformBTInfoProvider : BTInfoProvider {

    override fun registerCallback(callback: (Boolean) -> Unit): Long
    override fun unregisterCallback(caller: Long)

    override fun isBluetoothActive(): Boolean
    override fun isLEConnectionAllowed(): Boolean
    override fun isPeripheralRoleSupported(): Boolean
}
