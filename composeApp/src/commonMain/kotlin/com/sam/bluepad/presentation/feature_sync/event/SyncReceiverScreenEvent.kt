package com.sam.bluepad.presentation.feature_sync.event

sealed interface SyncReceiverScreenEvent {
    data object StartSyncReceiver : SyncReceiverScreenEvent
    data object StopSyncReceiver : SyncReceiverScreenEvent
    data object DisconnectAndReset : SyncReceiverScreenEvent
    data object ShowSyncChangeList : SyncReceiverScreenEvent
}
