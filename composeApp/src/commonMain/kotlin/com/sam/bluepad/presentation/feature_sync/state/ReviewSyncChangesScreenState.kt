package com.sam.bluepad.presentation.feature_sync.state

import com.sam.bluepad.domain.sync_diff.SyncChanges
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ReviewSyncChangesScreenState(
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val syncList: ImmutableList<SyncChanges> = persistentListOf()
)
