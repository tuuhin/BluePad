package com.sam.bluepad.presentation.feature_sync.state

import com.sam.bluepad.domain.models.ExternalDeviceModel

data class SyncReceiverScreenState(
    val currentDevice: ExternalDeviceModel? = null,
    val foreignDevice: ExternalDeviceModel? = null,
    val isReceiverRunning: Boolean = false,
)