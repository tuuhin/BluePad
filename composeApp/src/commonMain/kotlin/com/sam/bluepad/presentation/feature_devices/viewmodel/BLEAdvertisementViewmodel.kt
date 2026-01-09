package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.presentation.feature_devices.events.AdvertisementScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEAdvertisementViewmodel(
	private val bleAdvertisementManager: BLEAdvertisementManager,
) : AppViewModel() {

	private val _errorMessage = MutableStateFlow<String?>(null)
	val errorMessage = _errorMessage.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	val isAdvertisementRunning = bleAdvertisementManager.isRunning.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = false
	)

	fun onEvent(event: AdvertisementScreenEvent) {
		when (event) {
			AdvertisementScreenEvent.OnStartAdvertise -> onStart()
			AdvertisementScreenEvent.OnStopAdvertise -> bleAdvertisementManager.stopAdvertising()
		}
	}

	private fun onStart() = viewModelScope.launch {
		val result = bleAdvertisementManager.startAdvertising()

		result.fold(
			onSuccess = {
				_errorMessage.update { null }
			},
			onFailure = { err ->
				val message = err.message ?: "Some Error"
				_errorMessage.update { message }
			},
		)
	}


	override fun onCleared() {
		bleAdvertisementManager.cleanUp()
	}
}