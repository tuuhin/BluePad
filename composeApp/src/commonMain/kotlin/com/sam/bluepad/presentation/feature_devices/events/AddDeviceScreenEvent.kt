package com.sam.bluepad.presentation.feature_devices.events

import com.sam.bluepad.domain.ble.models.BLEPeerDevice

sealed interface AddDeviceScreenEvent {
    data object OnStartDeviceScan : AddDeviceScreenEvent
    data object OnStopDeviceScan : AddDeviceScreenEvent
    data object OnRefreshDeviceList : AddDeviceScreenEvent

    // bond state reader
    data class CheckBondStateForDevice(val device: BLEPeerDevice) : AddDeviceScreenEvent
}
