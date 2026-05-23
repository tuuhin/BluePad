package com.sam.bluepad.presentation.feature_sync.event

import kotlin.uuid.Uuid

sealed interface SyncChangesScreenEvent {
    data object OnApproveAction : SyncChangesScreenEvent
    data object OnCancelAction : SyncChangesScreenEvent
    data class OnResolveConflict(val identity: Uuid, val keepIncoming: Boolean) : SyncChangesScreenEvent
}
