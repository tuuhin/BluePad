package com.sam.bluepad.presentation.feature_sync.event

sealed interface SyncReceiverScreenEvent {
    data object OnStartSyncConnection : SyncReceiverScreenEvent
    data object OnRejectSyncConnection : SyncReceiverScreenEvent
    data object StartSyncReceiver : SyncReceiverScreenEvent
    data object StopSyncReceiver : SyncReceiverScreenEvent
}