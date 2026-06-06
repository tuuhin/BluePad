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

    val title: String
        @Composable
        get() = when (this) {
            is NotRunning -> "Ready to Sync"
            is Started -> "Establishing Connection"
            is Running -> "Transferring Data"
            is HalfDuplexCompleted -> "Finalizing Sync"
            is FullSyncSuccessFull -> "Sync Complete"
            is Failed -> "Sync Failed"
        }


    val description: String
        @Composable
        get() = when (this) {
            is NotRunning -> "Sync is currently idle."
            is Started -> "Connecting to the other device. Please wait..."
            is Running -> "Sending data to the remote device..."
            is HalfDuplexCompleted -> "Data sent successfully. Receiving updates from the other device..."
            is FullSyncSuccessFull -> "Success! Both devices are now up to date."
            is Failed -> "An error occurred: $message"
        }

    val connectionStatus: String
        @Composable
        get() = when (this) {
            NotRunning -> "Disconnected"
            Started -> "Handshaking..."
            Running, HalfDuplexCompleted -> "Connected"
            FullSyncSuccessFull -> "Finished"
            is Failed -> "Failed"
        }

}
