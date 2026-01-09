package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_devices.events.ConnectDeviceScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class BLEConnectDeviceViewmodel(
	private val address: String,
	private val connector: BLEConnectionManager
) : AppViewModel() {

	val connectionState = connector.isDeviceConnected
		.onStart { onConnectToDevice(address) }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(2_000),
			initialValue = BLEConnectionState.UNKNOWN
		)

	private val _peerData = MutableStateFlow<BLEPeerData?>(null)
	val peerData = _peerData.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	private var _connectJob: Job? = null

	fun onEvent(event: ConnectDeviceScreenEvent) {
		when (event) {
			ConnectDeviceScreenEvent.OnDisconnect -> onDisconnectDevice()
			ConnectDeviceScreenEvent.OnRetryConnection -> onConnectToDevice(address)
			ConnectDeviceScreenEvent.OnSaveDevice -> {}
		}
	}

	private fun onConnectToDevice(address: String) {
		_connectJob = connector.connectToDeviceAndRetrieveData(address).onEach { res ->
			when (res) {
				is Resource.Error -> {}
				is Resource.Success -> _peerData.update { res.data }
				Resource.Loading -> {}
			}
		}.launchIn(viewModelScope)
	}

	private fun onDisconnectDevice() {
		connector.disconnectAndClose()
		_connectJob?.cancel()
		_connectJob = null
	}

	override fun onCleared() {
		// cancel the scan job
		_connectJob?.cancel()
		_connectJob = null
		// disconnect andy connection if present
		connector.disconnectAndClose()
	}
}