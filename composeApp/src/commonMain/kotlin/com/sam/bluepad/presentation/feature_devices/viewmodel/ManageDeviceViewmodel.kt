package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_devices.events.ManageDevicesScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageDeviceViewmodel(
	private val repository: ExternalDevicesRepository,
) : AppViewModel() {

	private val _devices = MutableStateFlow(emptyList<ExternalDeviceModel>())
	val devices = _devices
		.onStart { loadDevicesList() }
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000L),
			initialValue = persistentListOf()
		)

	private val _isLoading = MutableStateFlow(true)
	val isLoading = _isLoading.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	fun onEvent(event: ManageDevicesScreenEvent) {
		when (event) {
			is ManageDevicesScreenEvent.OnRevokeDevice -> onRevokeDevice(event.device)
			is ManageDevicesScreenEvent.OnSyncDevice -> onSyncDevice(event.device)
		}
	}

	private fun loadDevicesList() = repository.getAllDevices()
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Some error"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
					_isLoading.update { false }
				}

				is Resource.Success -> {
					_devices.update { res.data }
					_isLoading.update { false }
				}

				Resource.Loading -> _isLoading.update { true }
			}
		}
		.launchIn(viewModelScope)

	private fun onRevokeDevice(device: ExternalDeviceModel) =
		repository.toggleDeviceRevocation(device)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some error"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> {
						val message = "${device.displayName} has been revoked"
						_uiEvents.emit(UIEvents.ShowToast(message))
					}

					else -> {}
				}
			}.launchIn(viewModelScope)

	private fun onSyncDevice(device: ExternalDeviceModel) = viewModelScope.launch {
		_uiEvents.emit(UIEvents.ShowSnackBar("Feature unavailable sorry"))
	}

}