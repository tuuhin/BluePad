package com.sam.bluepad.presentation.feature_devices.events

import com.sam.bluepad.domain.ble.models.BLEPeerDevice

sealed interface ScanDevicesNavEvent {
    data class NavigateToConnect(val device: BLEPeerDevice) : ScanDevicesNavEvent
    data class NavigateToCreateBond(val device: BLEPeerDevice) : ScanDevicesNavEvent
}
