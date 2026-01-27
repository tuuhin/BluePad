package com.sam.bluepad.presentation.feature_sync.event

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface ReceiveDeviceSyncScreenEvents {
	data class OnSelectDevice(val device: ExternalDeviceModel) : ReceiveDeviceSyncScreenEvents
	data object ToggleReceiver : ReceiveDeviceSyncScreenEvents
}