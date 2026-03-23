package com.sam.bluepad.presentation.feature_sync.state

import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel

data class SyncReceiverScreenState(
    val currentDevice: LocalDeviceInfoModel? = null,
    val localDevicePlatformOS: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    val foreignDevice: ExternalDeviceModel? = null,
    val isReceiverRunning: Boolean = false,
    val syncPhase: SyncUIState = SyncUIState.NotRunning,
)
