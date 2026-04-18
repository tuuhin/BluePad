package com.sam.bluepad.presentation.feature_sync.state

import androidx.compose.runtime.Composable

sealed class SyncUIState {
    data object NotRunning : SyncUIState()
    data object Started : SyncUIState()
    data object Running : SyncUIState()
    data object HalfDuplexCompleted : SyncUIState()
    data object FullSyncSuccessFull : SyncUIState()
    data class Failed(val message: String) : SyncUIState()

    /**
     * Running phase means we are sending or extracting data from other device
     * Half duplex means we have extracted/send now do the reverse with the devices
     */
    val isSyncing: Boolean
        get() = this is Running || this is HalfDuplexCompleted


    val receiverTitleText: String
        @Composable
        get() = when (this) {
            is Failed -> "Sync Failed"
            FullSyncSuccessFull -> "Sync Completed"
            HalfDuplexCompleted -> "Receiving data from other deivce"
            NotRunning -> "Sync Not Started"
            Running -> "Sending data to other device"
            Started -> "Sync Started"
        }

    val receiverDescText: String
        @Composable
        get() = when (this) {
            is Failed -> "Failed :${this.message}"
            FullSyncSuccessFull -> "Sync completed both of the devices have exchanged the data"
            HalfDuplexCompleted -> "Current device data is send waiting for data from other device"
            NotRunning -> "Sync is not running"
            Running -> "Sending data to the foreign device"
            Started -> "Sync Started , waiting for handshake to complete"
        }
}
