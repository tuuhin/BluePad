package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.models.BLEDeviceSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_sync.event.SyncConnectorScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ConnectorDiscoveryState
import com.sam.bluepad.presentation.feature_sync.state.SyncConnectorScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SyncConnectorViewModel(
    private val connectionManager: BLESyncConnectionManager
) : AppViewModel() {

    private val _isConnectorRunning = MutableStateFlow(false)
    private val _foreignDevice = MutableStateFlow<ExternalDeviceModel?>(null)
    private val _isAckReceived = MutableStateFlow(false)
    private val _discoveryState = MutableStateFlow(ConnectorDiscoveryState.DISCONNECTED)

    val screenState = combine(
        _isConnectorRunning,
        _foreignDevice,
        _isAckReceived,
        _discoveryState
    ) { running, device, isAckReceived, discovery ->
        SyncConnectorScreenState(
            isConnectorRunning = running,
            syncDevice = device,
            deviceDiscoveryState = discovery,
            isConnAckReceived = isAckReceived,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SyncConnectorScreenState()
    )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private var _connectionJob: Job? = null

    fun onEvent(event: SyncConnectorScreenEvent) {
        when (event) {
            SyncConnectorScreenEvent.StartClientConnection -> startConnection()
            SyncConnectorScreenEvent.StopClientConnection -> stopConnection()
            SyncConnectorScreenEvent.StartSync -> {}
            SyncConnectorScreenEvent.StopSync -> {}
        }
    }

    private fun startConnection() {
        _connectionJob?.cancel()
        _connectionJob = connectionManager.discoverAndConnect()
            .onStart { _isConnectorRunning.value = true }
            .onCompletion { _isConnectorRunning.value = false }
            .onEach(::handleConnEvents)
            .launchIn(viewModelScope)
    }

    private fun stopConnection() {
        _connectionJob?.cancel()
        _connectionJob = null
    }

    private suspend fun handleConnEvents(res: Resource<BLEDeviceSyncEvent, Exception>) {
        when (res) {
            is Resource.Error -> {
                // clear the fields
                _foreignDevice.update { null }
                _isAckReceived.update { false }
                // handle error event
                val message = res.message ?: res.error.message ?: "Some error"
                _uiEvents.emit(UIEvents.ShowSnackBar(message))
            }

            is Resource.Success -> when (res.data) {
                is BLEDeviceSyncEvent.AdvertisingAcknowledgmentReceived -> _isAckReceived.update { true }
                is BLEDeviceSyncEvent.AdvertisingDataRead -> _foreignDevice.update { res.data.device }
                BLEDeviceSyncEvent.ConnectionSuccess -> _discoveryState.update { ConnectorDiscoveryState.DISCOVERED }
                BLEDeviceSyncEvent.DeviceDisconnected -> _discoveryState.update { ConnectorDiscoveryState.DISCONNECTED }
                BLEDeviceSyncEvent.DeviceScanTimeout -> _discoveryState.update { ConnectorDiscoveryState.TIMEOUT }
                BLEDeviceSyncEvent.DiscoveryStarted -> _discoveryState.update { ConnectorDiscoveryState.DISCOVERING }
                else -> {}
            }

            else -> {}
        }
    }


    override fun onCleared() {
        stopConnection()
    }
}