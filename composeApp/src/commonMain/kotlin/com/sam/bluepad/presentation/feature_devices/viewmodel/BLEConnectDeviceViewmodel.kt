package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.mappers.toExternalDevice
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.enums.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_devices.events.ConnectDeviceScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEConnectDeviceViewmodel(
	private val address: String,
	private val connector: BLEConnectionManager,
	private val repository: ExternalDevicesRepository,
) : AppViewModel() {

	private val _connectedPeer = MutableStateFlow<BLEPeerData?>(null)
	val connectedPeerData = _connectedPeer.asStateFlow()

	private val _errorMessages = MutableSharedFlow<String?>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val errorMessage = _errorMessages.asSharedFlow()

	val connectionState = connector.connectionState
		.onStart { onConnectToDevice(address) }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(2_000),
			initialValue = BLEConnectionState.CONNECTING
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	fun onEvent(event: ConnectDeviceScreenEvent) {
		when (event) {
			ConnectDeviceScreenEvent.OnDisconnect -> onDisconnectDevice()
			ConnectDeviceScreenEvent.OnRetryConnection -> onConnectToDevice(address)
			ConnectDeviceScreenEvent.OnSaveDevice -> onSaveDevice()
		}
	}

	private fun onConnectToDevice(address: String) {
		// clear the connected pair
		_connectedPeer.value = null
		_errorMessages.tryEmit(null)

		connector.connectAndReceiveData(address = address, disconnectOnDone = false)
			.onEach { res ->
				when (res) {
					is Resource.Success -> _connectedPeer.update { res.data }
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some error"
						_errorMessages.tryEmit(message)
					}

					Resource.Loading -> {
						_uiEvents.emit(UIEvents.ShowToast("Connection Initiated"))
					}
				}
			}.launchIn(viewModelScope)
	}

	private fun onSaveDevice() {
		val peerData = _connectedPeer.value ?: return
		val externalDevice = peerData.toExternalDevice()
		repository.saveOrUpdateDevice(externalDevice).onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Some error"
					_errorMessages.tryEmit(message)
				}

				is Resource.Success -> {
					_uiEvents.emit(UIEvents.ShowToast("Device Saved"))
					_uiEvents.emit(UIEvents.PopScreen)
				}

				else -> {}
			}
		}.launchIn(viewModelScope)

	}

	private fun onDisconnectDevice() = viewModelScope.launch {
		_connectedPeer.value = null
		connector.disconnect()
	}

	override fun onCleared() {
		// disconnect andy connection if present and clean up
		connector.cleanUp()
	}
}