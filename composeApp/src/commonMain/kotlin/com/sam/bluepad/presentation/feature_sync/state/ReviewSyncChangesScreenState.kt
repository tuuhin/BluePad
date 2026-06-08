package com.sam.bluepad.presentation.feature_sync.state

import com.sam.bluepad.domain.sync_diff.SyncChanges
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ReviewSyncChangesScreenState(
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
    val saveState: ContentSaveState = ContentSaveState.NotSaved,
    val changesList: ImmutableList<ApprovedSyncChanges> = persistentListOf()
)

data class ApprovedSyncChanges(
    val isApproved: Boolean = true,
    // fields only used for conflicts
    val conflictResolution: ConflictResolutionState = ConflictResolutionState.NOT_SELECTED,
    val change: SyncChanges
)

enum class ConflictResolutionState {
    KEEP_LOCAL,
    KEEP_REMOTE,
    NOT_SELECTED
}

sealed class ContentSaveState {
    data object NotSaved : ContentSaveState()
    data object Saved : ContentSaveState()
    data object NothingToSave : ContentSaveState()
    data object Saving : ContentSaveState()
}

fun SyncChanges.toApprovedModel() = ApprovedSyncChanges(change = this)
