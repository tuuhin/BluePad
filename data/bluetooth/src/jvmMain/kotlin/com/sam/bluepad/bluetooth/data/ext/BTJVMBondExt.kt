package com.sam.bluepad.bluetooth.data.ext

import com.sam.bt_common.models.BTJVMBondResult

internal val BTJVMBondResult.description: String?
    get() = when (this) {
        BTJVMBondResult.NOT_READY_TO_PAIR -> "Device not ready"
        BTJVMBondResult.CONNECTION_REJECTED -> "Device rejected connection"
        BTJVMBondResult.TOO_MANY_CONNECTION -> "Too many devices trying to connect"
        BTJVMBondResult.HARDWARE_FAILURE -> "Hardware is unable to connect to the other device"
        BTJVMBondResult.AUTHENTICATION_TIMEOUT -> "Authentication timeout"
        BTJVMBondResult.AUTHENTICATION_NOT_ALLOWED -> "Authentication not allowed"
        BTJVMBondResult.AUTHENTICATION_FAILURE -> "Authentication failed"
        BTJVMBondResult.NO_SUPPORTED_PROFILES -> "Bluetooth profile not supported"
        BTJVMBondResult.PROTECTION_LEVEL_ISSUES, BTJVMBondResult.ACCESS_DENIED -> "Bluetooth access denied"
        BTJVMBondResult.PARING_OPERATION_CANCELLED, BTJVMBondResult.ERROR_OPERATION_CANCELLED -> "Paring cancelled"
        BTJVMBondResult.INVALID_DATA -> "Invalids result"
        BTJVMBondResult.HANDLER_NOT_REGISTERED -> "Pairing receiver not found (dev)"
        BTJVMBondResult.REJECTED_BY_HANDLER -> "Pairing rejected by handler"
        BTJVMBondResult.FAILED, BTJVMBondResult.ERROR_UNKNOWN -> "Failed to receive bond data"
        BTJVMBondResult.OPERATION_IN_PROGRESS -> "Operation in progress"
        else -> null
    }
