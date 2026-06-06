package com.sam.bluepad.presentation.feature_sync.event

import com.sam.bluepad.presentation.feature_sync.state.ConflictResolutionState
import kotlin.uuid.Uuid

sealed interface SyncChangesScreenEvent {

    data class OnApproveOrRejectChange(val identity: Uuid, val isApproved: Boolean) : SyncChangesScreenEvent
    data object OnApproveAll : SyncChangesScreenEvent
    data class OnResolveConflict(
        val identity: Uuid,
        val conflictResolutionState: ConflictResolutionState = ConflictResolutionState.KEEP_LOCAL
    ) : SyncChangesScreenEvent

    data object OnViewItems : SyncChangesScreenEvent
    data object OnApproveSave : SyncChangesScreenEvent
    data object OnCancelAction : SyncChangesScreenEvent
}
