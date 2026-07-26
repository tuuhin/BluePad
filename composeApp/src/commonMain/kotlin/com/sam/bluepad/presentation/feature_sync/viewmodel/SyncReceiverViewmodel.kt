package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.event.SyncWorkflowEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class SyncReceiverViewmodel(
    localDeviceProvider: LocalDeviceInfoProvider,
    platformProvider: PlatformInfoProvider,
    private val advertiser: BLEAdvertisementManager,
) : AppViewModel() {

    private val _foreignDevice = MutableStateFlow<ExternalDeviceModel?>(null)
    private val _syncPhase = MutableStateFlow<SyncUIState>(SyncUIState.NotRunning)
    private val _syncSessionId = MutableStateFlow<Uuid?>(null)

    val screenState = combine(
        localDeviceProvider.readDeviceInfo,
        _foreignDevice,
        _syncPhase,
        advertiser.isRunning,
    ) { localDevice, foreignDevice, syncPhase, isReceiverRunning ->
        SyncReceiverScreenState(
            currentDevice = localDevice,
            localDevicePlatformOS = platformProvider.platformOS,
            foreignDevice = foreignDevice,
            syncPhase = syncPhase,
            isReceiverRunning = isReceiverRunning,
        )
    }.onStart { readIncomingSyncEvents() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SyncReceiverScreenState(),
        )

    private var _eventsJob: Job? = null

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private val _workflowEvent = MutableSharedFlow<SyncWorkflowEvent>()
    val workflowEvent = _workflowEvent.asSharedFlow()

    fun onEvent(event: SyncReceiverScreenEvent) {
        when (event) {
            SyncReceiverScreenEvent.StartSyncReceiver -> onStartReceiver()
            SyncReceiverScreenEvent.StopSyncReceiver -> onStopReceiver()
            SyncReceiverScreenEvent.DisconnectAndReset -> onDisconnectAndReset()
            SyncReceiverScreenEvent.ShowSyncChangeList -> viewModelScope.launch {
                val sessionId = _syncSessionId.value ?: return@launch
                _workflowEvent.emit(SyncWorkflowEvent.ReadyForReview(sessionId))
            }
        }
    }

    private fun onStartReceiver() = viewModelScope.launch {
        if (screenState.value.isReceiverRunning) return@launch

        _foreignDevice.update { null }

        // start the advertisement
        advertiser.startAdvertising(BLEConnectionType.PROXIMITY_AND_SYNC)
    }

    private fun onStopReceiver() {
        if (!screenState.value.isReceiverRunning) return
        advertiser.stopAdvertising()
    }

    private fun readIncomingSyncEvents() {
        _eventsJob?.cancel()
        _eventsJob = advertiser.serverSyncEvents.onEach { event ->
            when (event) {
                is AdvertiserSyncEvent.HandshakeFailed -> _uiEvents.emit(UIEvents.ShowSnackBar(event.message))
                is AdvertiserSyncEvent.HandshakeSuccess -> _foreignDevice.update { event.device }
                is AdvertiserSyncEvent.SyncFailed -> _syncPhase.update { SyncUIState.Failed(event.reason) }
                is AdvertiserSyncEvent.SyncStarted -> {
                    _foreignDevice.update { event.device }
                    _syncPhase.update { SyncUIState.Running }
                }
                is AdvertiserSyncEvent.HalfDuplexCompleted -> _syncPhase.update { SyncUIState.HalfDuplexCompleted }
                is AdvertiserSyncEvent.FullDuplexCompleted -> {
                    _syncSessionId.update { event.sessionId }
                    _syncPhase.update { SyncUIState.FullSyncSuccessFull }
                }

                AdvertiserSyncEvent.HandshakeStarted -> _syncPhase.update { SyncUIState.Started }
            }
        }.catch { err ->
            val message = err.message ?: "Unknown error with receiving data"
            if (err !is CancellationException)
                _uiEvents.emit(UIEvents.ShowSnackBar(message))
        }.launchIn(viewModelScope)
    }

    private fun onDisconnectAndReset() {
        // cancel the job
        _eventsJob?.cancel()
        _eventsJob = null
        // update the fields
        _foreignDevice.update { null }
        _syncPhase.update { SyncUIState.NotRunning }
        // stop the advertisement
        advertiser.stopAdvertising()
    }

    override fun onCleared() {
        advertiser.cleanUp()
        // clean up
        _eventsJob?.cancel()
        _eventsJob = null
    }
}
