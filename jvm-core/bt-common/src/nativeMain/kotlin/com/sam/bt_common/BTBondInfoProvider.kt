package com.sam.bt_common

interface BTBondInfoProvider {

    val canReadBondInfo: Boolean

    val canShowConfirmPinDialog: Boolean

    fun checkDeviceBondState(address: String): Int

    fun registerForBondConfirmPin(
        address: String,
        onConfirmPin: (pin: String) -> Unit,
        onResponse: (Int) -> Unit,
        onError: (Int) -> Unit
    )

    fun unregisterForBondConfirmPin()

    fun acceptConfirmPin(pin: String)
}
