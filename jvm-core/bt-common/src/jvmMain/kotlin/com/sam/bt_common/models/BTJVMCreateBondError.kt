package com.sam.bt_common.models

enum class BTJVMCreateBondError(val code: Int) {
    ERROR_DEVICE_ALREADY_BONDED(1),
    ERROR_DEVICE_CANNOT_BE_BONDED(2);

    companion object {
        fun fromInt(code: Int): BTJVMCreateBondError {
            return BTJVMCreateBondError.entries.find { it.code == code }
                ?: ERROR_DEVICE_ALREADY_BONDED
        }
    }
}
