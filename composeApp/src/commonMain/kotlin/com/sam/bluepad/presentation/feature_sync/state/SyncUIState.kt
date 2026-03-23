package com.sam.bluepad.presentation.feature_sync.state

sealed class SyncUIState {
    data object NotRunning : SyncUIState()
    data object Running : SyncUIState()
    data object Completed : SyncUIState()
    data class Failed(val message: String) : SyncUIState()
}
