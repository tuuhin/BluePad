package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.models.BLEServerSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
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
    private val advertiser: BLEAdvertisementManager,
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val platformProvider: PlatformInfoProvider,
) : AppViewModel() {

    private val _currentDevice = MutableStateFlow<ExternalDeviceModel?>(null)
    private val _foreignDevice = MutableStateFlow<ExternalDeviceModel?>(null)

    val screenState = combine(
        _currentDevice,
        _foreignDevice,
        advertiser.isRunning
    ) { current, foreign, isRunning ->
        SyncReceiverScreenState(
            currentDevice = current,
            foreignDevice = foreign,
            isReceiverRunning = isRunning
        )
    }.onStart {
        readSyncProximityEvents()
        readLocalDevice()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SyncReceiverScreenState()
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

    private fun onRejectSyncConnection() {
        _foreignDevice.update { null }
    }

    private fun onStartReceiver() = viewModelScope.launch {
        if (screenState.value.isReceiverRunning) return@launch
        advertiser.startAdvertising(BLEConnectionType.PROXIMITY_AND_SYNC)
        _foreignDevice.update { null }
    }

    private fun onStopReceiver() {
        if (!screenState.value.isReceiverRunning) return
        advertiser.stopAdvertising()
    }

    private fun readSyncProximityEvents() = advertiser.serverSyncEvents
        .onEach { event ->
            when (event) {
                is BLEServerSyncEvent.SyncRequest -> {
                    _foreignDevice.update { event.device }
                    advertiser.stopAdvertising()
                }
            }
        }.launchIn(viewModelScope)


    private fun readLocalDevice() = localDeviceProvider.readDeviceInfo
        .onEach { device ->
            val localToExternal = ExternalDeviceModel(
                id = device.deviceId,
                displayName = device.name,
                deviceOs = platformProvider.platformOS
            )
            _currentDevice.update { localToExternal }
        }.launchIn(viewModelScope)


    override fun onCleared() {
        advertiser.cleanUp()
    }
}