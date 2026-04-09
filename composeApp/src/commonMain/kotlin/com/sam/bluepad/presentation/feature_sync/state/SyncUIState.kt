package com.sam.bluepad.presentation.feature_sync.state

sealed class SyncUIState {
    data object NotRunning : SyncUIState()
    data object Started : SyncUIState()
    data object Running : SyncUIState()
    data object HalfDuplex : SyncUIState()
    data object FullDuplex : SyncUIState()
    data class Failed(val message: String) : SyncUIState()
}
