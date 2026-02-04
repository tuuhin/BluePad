package com.sam.bluepad.presentation.feature_sync.event

sealed interface SyncConnectorScreenEvent {
    data object StartClientConnection : SyncConnectorScreenEvent
    data object StopClientConnection : SyncConnectorScreenEvent
    data object StartSync : SyncConnectorScreenEvent
    data object StopSync : SyncConnectorScreenEvent
}