package com.sam.bluepad.presentation.feature_settings

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
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
import kotlinx.coroutines.launch

class SettingsViewmodel(
	private val deviceInfoProvider: LocalDeviceInfoProvider,
	private val repository: ExternalDevicesRepository,
) : AppViewModel() {

	private val _settingsState = MutableStateFlow(SettingsScreenState())
	val screenState = _settingsState.asStateFlow()

	private val _isLoading = MutableStateFlow(true)
	val isLoading = _isLoading
		.onStart { onLoadData() }
		.stateIn(viewModelScope, SharingStarted.Lazily, true)

	private val _uiEvent = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvent

	fun onEvent(event: SettingsScreenEvent) {
		when (event) {
			is SettingsScreenEvent.OnUpdateDeviceName -> onUpdateDeviceName(event.name)
			SettingsScreenEvent.OnReEnrollRevokeDevices -> onUnRevokeAllDevice()
		}
	}

	private fun onUnRevokeAllDevice() = repository.unRevokeAllDevices()
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Some error"
					_uiEvent.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> _uiEvent.emit(UIEvents.ShowToast("All device un-revoked"))
				else -> {}
			}
		}.launchIn(viewModelScope)


	private fun onUpdateDeviceName(name: String) = viewModelScope.launch {
		deviceInfoProvider.updateDeviceName(name)
		_uiEvent.emit(UIEvents.ShowSnackBar("Name updated"))
	}

	private fun onLoadData() {
		deviceInfoProvider.readDeviceInfo.onEach {
			_settingsState.update { state -> state.copy(deviceName = it.name) }
			_isLoading.update { false }
		}.launchIn(viewModelScope)
	}
}