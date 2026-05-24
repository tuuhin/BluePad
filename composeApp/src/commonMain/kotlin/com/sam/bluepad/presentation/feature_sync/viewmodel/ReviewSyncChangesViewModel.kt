package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.sync_diff.SyncDiffReviewManager
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ReviewSyncChangesScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ReviewSyncChangesViewModel(
    private val sessionId: Uuid,
    private val reviewManager: SyncDiffReviewManager
) : AppViewModel() {

    private val _syncDiffs = MutableStateFlow(ReviewSyncChangesScreenState())
    val screenState = _syncDiffs
        .onStart { readSyncDiffs() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ReviewSyncChangesScreenState(),
        )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    fun onEvent(event: SyncChangesScreenEvent) {
        when (event) {
            SyncChangesScreenEvent.OnApproveAction -> saveChanges()
            SyncChangesScreenEvent.OnCancelAction -> viewModelScope.launch {
                _uiEvents.emit(UIEvents.PopScreen)
            }

            is SyncChangesScreenEvent.OnResolveConflict -> {
                // TODO: Implement conflict resolution logic in reviewManager
            }
        }
    }

    private fun readSyncDiffs() = viewModelScope.launch {
        val result = reviewManager.readSyncSession(sessionId)
        result.fold(
            onSuccess = { changes ->
                val syncUiChanges = changes.toImmutableList()
                _syncDiffs.update { state -> state.copy(isLoaded = true, syncList = syncUiChanges) }
            },
            onFailure = { err ->
                _syncDiffs.update { state -> state.copy(isLoaded = true, errorMessage = err.message) }
            },
        )
    }

    private fun saveChanges() = viewModelScope.launch {
        _syncDiffs.update { state -> state.copy(isSaving = true) }
    }
}
