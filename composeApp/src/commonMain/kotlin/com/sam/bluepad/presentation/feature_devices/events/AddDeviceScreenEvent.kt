package com.sam.bluepad.presentation.feature_devices.events

sealed interface AddDeviceScreenEvent {
	data object OnStartDeviceScan : AddDeviceScreenEvent
	data object OnStopDeviceScan : AddDeviceScreenEvent
	data object OnRefreshDeviceList : AddDeviceScreenEvent
}
