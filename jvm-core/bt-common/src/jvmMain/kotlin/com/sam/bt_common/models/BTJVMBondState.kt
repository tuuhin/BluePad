package com.sam.bt_common.models

enum class BTJVMBondState(val code: Int) {
    DEVICE_BONDED(1),
    DEVICE_NOT_BONDED(2),
    ERROR_INVALID_DEVICE(3),
    ERROR_DEVICE_CANNOT_PAIR(4),
    ERROR_UNKNOWN(5);

    companion object {
        fun fromStatus(status: Int): BTJVMBondState =
            BTJVMBondState.entries.find { it.code == status } ?: ERROR_UNKNOWN
    }
}
