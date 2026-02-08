package com.sam.bluepad.presentation.feature_sync.state

import androidx.compose.runtime.Stable
import com.sam.bluepad.domain.models.ExternalDeviceModel

@Stable
data class SyncConnectorScreenState(
    val isConnectorRunning: Boolean = false,
    val syncDevice: ExternalDeviceModel? = null,
    val isConnAckReceived: Boolean = false,
    val discoveryState: ConnectorDiscoveryState = ConnectorDiscoveryState.DISCONNECTED,
) {
    val isReadyToSync: Boolean
        get() = syncDevice != null && isConnAckReceived
}