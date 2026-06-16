package com.sam.bt_common

expect class PlatformBondInfoProvider : BTBondInfoProvider {
    override fun checkDeviceBondState(address: String): Int
    override fun createBond(address: String, timeoutInMillis: Int): Int
}
