package com.sam.bluepad.presentation.feature_sync.event

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface ReceiveSyncEvent {
	data class OnSelectDevice(val device: ExternalDeviceModel) : ReceiveSyncEvent
	data object ToggleReceiver : ReceiveSyncEvent
}