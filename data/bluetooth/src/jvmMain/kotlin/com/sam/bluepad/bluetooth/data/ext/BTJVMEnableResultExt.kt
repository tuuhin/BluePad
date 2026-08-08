package com.sam.bluepad.bluetooth.data.ext

import com.sam.bt_common.models.BTJVMEnableResult

internal val BTJVMEnableResult.description: String
    get() = when (this) {
        BTJVMEnableResult.REQUEST_OPTION_NOT_FOUND -> "Option not found to enable use system settings"
        BTJVMEnableResult.REQUEST_ACCEPTED -> "Accepted"
        BTJVMEnableResult.REQUEST_DENIED_PRIVACY_ISSUES -> "Cannot request access privacy issues"
        BTJVMEnableResult.REQUEST_DENIED_BY_SYSTEM -> "Request denied by system"
        BTJVMEnableResult.REQUEST_DENIED_BY_USER -> "Request denied by user"
        BTJVMEnableResult.REQUEST_DENIED_CANNOT_FIND_ADAPTER -> "Missing bluetooth adapter"
        BTJVMEnableResult.REQUEST_DENIED_UNKNOWN -> "Request state cannot be determined"
        BTJVMEnableResult.REQUEST_NOT_NEEDED -> "Bluetooth is already enabled"
    }
