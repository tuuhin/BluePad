package com.sam.bt_common.models

enum class BTJVMEnableResult(val statusCode: Int) {
    REQUEST_OPTION_NOT_FOUND(-1),
    REQUEST_ACCEPTED(0),
    REQUEST_DENIED_PRIVACY_ISSUES(1),
    REQUEST_DENIED_BY_SYSTEM(2),
    REQUEST_DENIED_BY_USER(3),
    REQUEST_DENIED_CANNOT_FIND_ADAPTER(4),
    REQUEST_DENIED_UNKNOWN(5),
    REQUEST_NOT_NEEDED(6);

    companion object {
        fun fromInt(code: Int): BTJVMEnableResult {
            return BTJVMEnableResult.entries.find { it.statusCode == code }
                ?: REQUEST_DENIED_UNKNOWN
        }
    }
}
