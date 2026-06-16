package com.sam.bt_common

interface BTBondInfoProvider {
    fun checkDeviceBondState(address: String): Int
    fun createBond(address: String, timeoutInMillis: Int = 60 * 1000): Int
}
