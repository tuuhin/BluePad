package com.sam.bluepad.presentation.feature_sync.state

import androidx.compose.runtime.Immutable
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.LocalDeviceInfoModel

@Immutable
data class SyncConnectorScreenState(
    val isConnectorRunning: Boolean = false,
    val localDevice: LocalDeviceInfoModel? = null,
    val localDevicePlatformOS: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    val discoveryState: DiscoveryUIState = DiscoveryUIState.NotStarted,
    val syncState: SyncUIState = SyncUIState.NotRunning,
)
