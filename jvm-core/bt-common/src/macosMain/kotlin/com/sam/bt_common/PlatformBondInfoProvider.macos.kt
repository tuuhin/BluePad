package com.sam.bt_common

import com.sam.bt_common.exceptions.MacOsApiException

actual class PlatformBondInfoProvider : BTBondInfoProvider {

    actual override val canReadBondInfo: Boolean = false

    actual override val canShowConfirmPinDialog: Boolean = false

    actual override fun checkDeviceBondState(address: String): Int {
        // 5 will be mapped to error_unknown in bluetooth_bond_state enum
        return 5
    }

    actual override fun registerForBondConfirmPin(
        address: String,
        onConfirmPin: (pin: String) -> Unit,
        onResponse: (Int) -> Unit,
        onError: (Int) -> Unit
    ) {
        throw MacOsApiException()
    }

    actual override fun acceptConfirmPin(pin: String) {
        throw MacOsApiException()
    }

    actual override fun unregisterForBondConfirmPin() {
        throw MacOsApiException()
    }
}
