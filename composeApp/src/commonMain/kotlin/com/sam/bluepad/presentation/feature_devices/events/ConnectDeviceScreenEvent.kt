package com.sam.bluepad.presentation.feature_devices.events

sealed interface ConnectDeviceScreenEvent {
	data object OnDisconnect : ConnectDeviceScreenEvent
	data object OnRetryConnection : ConnectDeviceScreenEvent
	data object OnSaveDevice : ConnectDeviceScreenEvent
}