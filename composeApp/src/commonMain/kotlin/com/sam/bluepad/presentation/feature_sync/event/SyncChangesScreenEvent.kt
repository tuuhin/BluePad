package com.sam.bluepad.presentation.feature_sync.event

sealed interface SyncChangesScreenEvent {
    data object OnApproveAction : SyncChangesScreenEvent
    data object OnCancelAction : SyncChangesScreenEvent
}
