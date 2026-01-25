package com.sam.bluepad.presentation.feature_devices.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface BlackListDeviceScreenEvent {
	data class OnRestoreDevice(val device: ExternalDeviceModel) : BlackListDeviceScreenEvent
	data class OnDeleteDevice(val device: ExternalDeviceModel) : BlackListDeviceScreenEvent
	data object OnRestoreAllDevice : BlackListDeviceScreenEvent
}