package com.sam.bt_common

import com.sam.bt_common.platform.mingw.is_device_bonded
import com.sam.bt_common.platform.mingw.request_bond

actual class PlatformBondInfoProvider : BTBondInfoProvider {

    actual override val canReadBondInfo: Boolean = true

    actual override fun checkDeviceBondState(address: String): Int =
        is_device_bonded(address)

    actual override fun createBond(address: String, timeoutInMillis: Int): Int {
        // will block the thread for timeout ensure called from a non-main thread
        return request_bond(address, timeoutInMillis.toUInt())
    }
}
