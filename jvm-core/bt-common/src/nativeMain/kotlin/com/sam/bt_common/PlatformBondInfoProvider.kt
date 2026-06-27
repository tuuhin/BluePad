package com.sam.bt_common

expect class PlatformBondInfoProvider : BTBondInfoProvider {


    override val canReadBondInfo: Boolean
    override val canShowConfirmPinDialog: Boolean
    override fun checkDeviceBondState(address: String): Int

    override fun registerForBondConfirmPin(
        address: String,
        onConfirmPin: (pin: String) -> Unit,
        onResponse: (Int) -> Unit,
        onError: (Int) -> Unit
    )

    override fun acceptConfirmPin()

    override fun unregisterForBondConfirmPin()
}
