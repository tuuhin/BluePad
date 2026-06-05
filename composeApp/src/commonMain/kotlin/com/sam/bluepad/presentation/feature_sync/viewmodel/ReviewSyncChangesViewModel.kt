package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncDataSaver
import com.sam.bluepad.domain.sync_diff.SyncDataSessionReader
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.event.SyncWorkflowEvent
import com.sam.bluepad.presentation.feature_sync.state.ApprovedSyncChanges
import com.sam.bluepad.presentation.feature_sync.state.ConflictResolutionState
import com.sam.bluepad.presentation.feature_sync.state.ContentSaveState
import com.sam.bluepad.presentation.feature_sync.state.ReviewSyncChangesScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ReviewSyncChangesViewModel(
    private val sessionId: Uuid,
    private val deviceId: Uuid,
    private val reviewManager: SyncDataSessionReader,
    private val syncDataSaver: SyncDataSaver,
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

    private val _workFlowEvent = MutableSharedFlow<SyncWorkflowEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val workFlowEvent = _workFlowEvent.asSharedFlow()

    fun onEvent(event: SyncChangesScreenEvent) {
        when (event) {
            is SyncChangesScreenEvent.OnApproveOrRejectChange -> _syncDiffs.update { state ->
                val list = state.changesList
                    .map { item ->
                        if (item.change.identity != event.identity) item
                        else item.copy(isApproved = event.isApproved)
                    }
                state.copy(changesList = list.toImmutableList())
            }

            SyncChangesScreenEvent.OnApproveAll -> _syncDiffs.update { state ->
                val list = state.changesList
                    .map { item -> item.copy(isApproved = true) }
                state.copy(changesList = list.toImmutableList())
            }

            is SyncChangesScreenEvent.OnResolveConflict -> _syncDiffs.update { state ->
                val list = state.changesList
                    .map { item ->
                        // only taken into account for conflicts
                        if (item.change is SyncChanges.Conflict) {
                            val newState =
                                if (item.conflictResolution != ConflictResolutionState.NOT_SELECTED)
                                    ConflictResolutionState.NOT_SELECTED
                                else event.conflictResolutionState
                            item.copy(conflictResolution = newState)
                        } else item
                    }
                state.copy(changesList = list.toImmutableList())
            }

            is SyncChangesScreenEvent.OnApproveSave -> saveChanges(deviceId)
            SyncChangesScreenEvent.OnCancelAction -> viewModelScope.launch {
                _uiEvents.emit(UIEvents.PopScreen)
            }

            SyncChangesScreenEvent.OnViewItems -> _workFlowEvent.tryEmit(SyncWorkflowEvent.ReviewedAndSaved)
        }
    }

    private fun readSyncDiffs() = viewModelScope.launch {

        _syncDiffs.update { state -> state.copy(isLoaded = false) }

        val result = reviewManager.readSyncSession(sessionId)
        result.fold(
            onSuccess = { changes ->
                val syncUiChanges = changes
                    .map { ApprovedSyncChanges(change = it) }
                    .toImmutableList()

                _syncDiffs.update { state ->
                    state.copy(
                        isLoaded = true,
                        changesList = syncUiChanges,
                        saveState = if (syncUiChanges.isEmpty()) ContentSaveState.NothingToSave
                        else ContentSaveState.NotSaved,
                    )
                }
            },
            onFailure = { err ->
                _syncDiffs.update { state -> state.copy(isLoaded = true, errorMessage = err.message) }
            },
        )
    }

    private fun saveChanges(deviceId: Uuid) = viewModelScope.launch {
        _syncDiffs.update { state -> state.copy(saveState = ContentSaveState.Saving) }
        // only take the list of approved items
        val syncList = _syncDiffs.value.changesList
            .filter { it.isApproved }
            .map { it.change }

        if (syncList.isEmpty()) {
            _syncDiffs.update { state -> state.copy(saveState = ContentSaveState.NothingToSave) }
            return@launch
        }

        val result = syncDataSaver.submitSyncChanges(syncList, deviceId)
        result.fold(
            onSuccess = {
                _syncDiffs.update { state -> state.copy(saveState = ContentSaveState.Saved) }
                _uiEvents.emit(UIEvents.ShowSnackBar("Contents have been updated"))
            },
            onFailure = { err ->
                val message = err.message ?: "Unable to save the content"
                _syncDiffs.update { state ->
                    state.copy(
                        saveState = ContentSaveState.NotSaved,
                        errorMessage = message,
                    )
                }
            },
        )
    }
}
