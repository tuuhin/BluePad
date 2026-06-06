package com.sam.bluepad.presentation.feature_sync.state

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed class DiscoveryUIState {
    data object NotStarted : DiscoveryUIState()
    data object Discovering : DiscoveryUIState()
    data object Timeout : DiscoveryUIState()
    data object Discovered : DiscoveryUIState()
    data object Disconnected : DiscoveryUIState()
    data class DeviceConnected(val device: ExternalDeviceModel) : DiscoveryUIState()
}
