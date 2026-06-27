package com.sam.bt_common

interface BTBondInfoProvider {

    /**
     * Some platforms hind the bluetooth bond information, bond is automatically performed on
     * read,write on characteristics
     */
    val canReadBondInfo: Boolean

    /**
     * Checks if the platform provides the functionality to show a custom pin confirmation dialog
     */
    val canShowConfirmPinDialog: Boolean


    /**
     * Reads a random mac address anc checks if the device is bonded
     */
    fun checkDeviceBondState(address: String): Int

    fun registerForBondConfirmPin(
        address: String,
        onConfirmPin: (pin: String) -> Unit,
        onResponse: (Int) -> Unit,
        onError: (Int) -> Unit
    )

    fun unregisterForBondConfirmPin()

    fun acceptConfirmPin()
}
