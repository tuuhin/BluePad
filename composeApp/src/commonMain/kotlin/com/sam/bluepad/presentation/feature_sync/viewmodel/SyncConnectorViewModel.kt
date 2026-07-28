package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_sync.event.SyncConnectorScreenEvent
import com.sam.bluepad.presentation.feature_sync.event.SyncWorkflowEvent
import com.sam.bluepad.presentation.feature_sync.state.DiscoveryUIState
import com.sam.bluepad.presentation.feature_sync.state.SyncConnectorScreenState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class SyncConnectorViewModel(
    localDeviceInfoProvider: LocalDeviceInfoProvider,
    platformInfoProvider: PlatformInfoProvider,
    private val connectionManager: BLESyncConnectionManager
) : AppViewModel() {

    private val _isConnectorRunning = MutableStateFlow(false)

    private val _connectionState = MutableStateFlow<DiscoveryUIState>(DiscoveryUIState.NotStarted)
    private val _syncState = MutableStateFlow<SyncUIState>(SyncUIState.NotRunning)
    private val _syncSessionId = MutableStateFlow<Uuid?>(null)

    val screenState = combine(
        _isConnectorRunning,
        _connectionState,
        localDeviceInfoProvider.readDeviceInfo,
        _syncState,
    ) { running, discovery, localDevice, syncState ->
        SyncConnectorScreenState(
            isConnectorRunning = running,
            discoveryState = discovery,
            localDevice = localDevice,
            localDevicePlatformOS = platformInfoProvider.platformOS,
            syncState = syncState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SyncConnectorScreenState(),
    )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private val _workflowEvent = MutableSharedFlow<SyncWorkflowEvent>()
    val workflowEvent = _workflowEvent.asSharedFlow()

    private var _connectionJob: Job? = null

    fun onEvent(event: SyncConnectorScreenEvent) {
        when (event) {
            SyncConnectorScreenEvent.StartClientConnection -> startConnection()
            SyncConnectorScreenEvent.StopClientConnection -> stopConnection()
            SyncConnectorScreenEvent.ShowSyncChangesList -> viewModelScope.launch {
                val sessionId = _syncSessionId.value ?: return@launch
                _workflowEvent.emit(SyncWorkflowEvent.ReadyForReview(sessionId))
            }
        }
    }

    private fun startConnection() {
        _connectionJob?.cancel()
        _connectionJob = connectionManager.discoverAndConnect()
            .onStart { _isConnectorRunning.value = true }
            .onCompletion {
                _isConnectorRunning.value = false
                _connectionState.update { DiscoveryUIState.NotStarted }
            }
            .onEach { res ->
                when (res) {
                    is Resource.Error -> handleConnectionError(res.error, res.message)
                    is Resource.Success -> handleConnEvents(res.data)
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private fun stopConnection() {
        _connectionJob?.cancel()
        _connectionJob = null
    }

    private fun handleConnEvents(event: ConnectorSyncEvent) {
        when (event) {
            ConnectorSyncEvent.DiscoveryStarted -> _connectionState.update { DiscoveryUIState.Discovering }
            ConnectorSyncEvent.DeviceScanTimeout -> _connectionState.update { DiscoveryUIState.Timeout }
            is ConnectorSyncEvent.DeviceFound, ConnectorSyncEvent.ConnectionSuccess ->
                _connectionState.update { DiscoveryUIState.Discovered }

            ConnectorSyncEvent.DeviceDisconnected -> _connectionState.update { DiscoveryUIState.Disconnected }
            is ConnectorSyncEvent.HandshakeSuccess -> {
                _connectionState.update { DiscoveryUIState.DeviceConnected(event.device) }
                _syncState.update { SyncUIState.Started }
            }

            is ConnectorSyncEvent.FullDuplexCompleted -> {
                _syncSessionId.update { event.sessionId }
                _syncState.update { SyncUIState.FullSyncSuccessFull }
            }
            is ConnectorSyncEvent.HalfDuplexCompleted -> _syncState.update { SyncUIState.HalfDuplexCompleted }
            is ConnectorSyncEvent.SyncFailed -> _syncState.update { SyncUIState.Failed(event.reason) }
            is ConnectorSyncEvent.SyncStarted -> _syncState.update { SyncUIState.Running }
            is ConnectorSyncEvent.HandshakeFailed -> _syncState.update { SyncUIState.Failed(event.message ?: "") }
            else -> {}
        }
    }

    private fun handleConnectionError(error: Exception, message: String? = null) = viewModelScope.launch {
        val message = message ?: error.message ?: "Some error"

        _syncState.update { SyncUIState.Failed(message) }
        _connectionState.update { DiscoveryUIState.Disconnected }
        // handle error event
        _uiEvents.emit(UIEvents.ShowSnackBar(message))
    }

    override fun onCleared() {
        connectionManager.close()
        stopConnection()
    }
}
