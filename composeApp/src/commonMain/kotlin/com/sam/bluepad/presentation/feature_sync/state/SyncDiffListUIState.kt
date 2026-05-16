package com.sam.bluepad.presentation.feature_sync.state

import androidx.compose.runtime.Stable
import com.sam.bluepad.domain.sync_diff.SyncChanges
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class SyncChangeUIState(val changes: SyncChanges)

data class SyncDiffListUIState(
    val isLoaded: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val syncList: ImmutableList<SyncChangeUIState> = persistentListOf()
)
