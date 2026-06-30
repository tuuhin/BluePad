package com.sam.bluepad.presentation.feature_devices.state

import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AddDeviceScreenState(
    val peers: ImmutableList<BLEPeerDevice> = persistentListOf(),
    val isScanning: Boolean = false,
    val isListRefreshing: Boolean = false,
) {

    val isRefreshButtonEnabled: Boolean
        get() = peers.isNotEmpty() && !isListRefreshing && !isScanning
}
