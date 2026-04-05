package com.sam.bluepad.presentation.feature_devices.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface ManageDevicesScreenEvent {
	data class OnDeleteDevice(val device: ExternalDeviceModel) : ManageDevicesScreenEvent
	data class OnRevokeDevice(val device: ExternalDeviceModel) : ManageDevicesScreenEvent
}