package com.sam.bt_common

interface BTBondInfoProvider {

    /**
     * Not all platforms supports reading bluetooth bond info
     * So a flag to indicate which one does
     */
    val canReadBondInfo: Boolean

    fun checkDeviceBondState(address: String): Int
    fun createBond(address: String, timeoutInMillis: Int = 60 * 1000): Int
}
