package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncReceiverViewmodel(
    localDeviceProvider: LocalDeviceInfoProvider,
    private val advertiser: BLEAdvertisementManager,
    private val platformProvider: PlatformInfoProvider,
) : AppViewModel() {

    private val _foreignDevice = MutableStateFlow<ExternalDeviceModel?>(null)
    private val _syncPhase = MutableStateFlow<SyncUIState>(SyncUIState.NotRunning)

    val screenState = combine(
        localDeviceProvider.readDeviceInfo,
        _foreignDevice,
        _syncPhase,
        advertiser.isRunning,
    ) { current, foreign, isSyncRunning, isRunning ->
        SyncReceiverScreenState(
            currentDevice = current,
            localDevicePlatformOS = platformProvider.platformOS,
            foreignDevice = foreign,
            isReceiverRunning = isRunning,
            syncPhase = isSyncRunning,
        )
    }.onStart { readIncomingSyncEvents() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SyncReceiverScreenState(),
        )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents


    fun onEvent(event: SyncReceiverScreenEvent) {
        when (event) {
            SyncReceiverScreenEvent.OnStartSyncConnection -> onStartSync()
            SyncReceiverScreenEvent.StartSyncReceiver -> onStartReceiver()
            SyncReceiverScreenEvent.StopSyncReceiver -> onStopReceiver()
            SyncReceiverScreenEvent.OnRejectSyncConnection -> onRejectSyncConnection()
        }
    }

    private fun onStartSync() = viewModelScope.launch {
        _uiEvents.emit(UIEvents.ShowSnackBar("Feature unavailable"))
    }

    private fun onRejectSyncConnection() = _foreignDevice.update { null }

    private fun onStartReceiver() = viewModelScope.launch {
        if (screenState.value.isReceiverRunning) return@launch
        advertiser.startAdvertising(BLEConnectionType.PROXIMITY_AND_SYNC)
        _foreignDevice.update { null }
    }

    private fun onStopReceiver() {
        if (!screenState.value.isReceiverRunning) return
        advertiser.stopAdvertising()
    }

    private fun readIncomingSyncEvents() = advertiser.serverSyncEvents
        .onEach { event ->
            when (event) {
                is AdvertiserSyncEvent.HandshakeFailed -> _uiEvents.emit(UIEvents.ShowSnackBar(event.message))
                is AdvertiserSyncEvent.HandshakeSuccess -> _foreignDevice.update { event.device }
                is AdvertiserSyncEvent.SyncCompleted -> _syncPhase.update { SyncUIState.Completed }
                is AdvertiserSyncEvent.SyncFailed -> _syncPhase.update { SyncUIState.Failed(event.reason) }
                is AdvertiserSyncEvent.SyncStarted -> _syncPhase.update { SyncUIState.Running }
            }
        }.launchIn(viewModelScope)

    override fun onCleared() {
        advertiser.cleanUp()
    }
}
