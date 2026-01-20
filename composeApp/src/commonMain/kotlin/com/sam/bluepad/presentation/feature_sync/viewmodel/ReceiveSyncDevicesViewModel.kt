package com.sam.bluepad.presentation.feature_sync.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEAdvertisementType
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.presentation.feature_sync.event.ReceiveSyncEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceiveSyncDevicesViewModel(
	private val advertiser: BLEAdvertisementManager,
	private val repository: ExternalDevicesRepository,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	private val _savedDevices = MutableStateFlow<List<ExternalDeviceModel>>(emptyList())
	val savedDevices = _savedDevices
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000L),
			initialValue = persistentListOf()
		)

	private val _selectedDevice = MutableStateFlow<ExternalDeviceModel?>(null)
	val selectedDevice = _selectedDevice.asStateFlow()

	val isAdvertising = advertiser.isRunning
		.onStart { advertiser.startAdvertising(BLEAdvertisementType.PROXIMITY_AND_SYNC) }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	fun onEvent(event: ReceiveSyncEvent) {
		when (event) {
			is ReceiveSyncEvent.OnSelectDevice -> _selectedDevice.update { prev ->
				if (prev == event.device) null else event.device
			}

			ReceiveSyncEvent.ToggleReceiver -> viewModelScope.launch {
				if (isAdvertising.value) advertiser.stopAdvertising()
				else advertiser.startAdvertising(BLEAdvertisementType.PROXIMITY_AND_SYNC)
			}
		}
	}

	override fun onCleared() {
		advertiser.cleanUp()
	}
}