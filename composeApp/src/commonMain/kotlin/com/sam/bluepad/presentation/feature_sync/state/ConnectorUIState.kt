package com.sam.bluepad.presentation.feature_sync.state

import androidx.compose.runtime.Stable
import com.sam.bluepad.domain.models.ExternalDeviceModel

@Stable
sealed interface ConnectorUIState {

    // discovery
    data object Idle : ConnectorUIState
    data object Scanning : ConnectorUIState
    data object ScanTimeout : ConnectorUIState

    // connected
    data object DisconnectedWithoutData : ConnectorUIState
    data object ConnectedWithoutData : ConnectorUIState

    // device
    data class DeviceDataRead(val device: ExternalDeviceModel) : ConnectorUIState
}