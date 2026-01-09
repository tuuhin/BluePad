package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.presentation.feature_devices.events.AddDeviceScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEScanDevicesViewModel(
	private val bleScanManager: BLEDiscoveryManager,
) : AppViewModel() {

	private val _isListRefreshing = MutableStateFlow(false)
	val isListRefreshing = _isListRefreshing.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	val isScanning = bleScanManager.isScanning.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = false
	)

	val scanPeers = bleScanManager.scanResults
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = persistentListOf()
		)

	private var _bleScanJob: Job? = null

	fun onEvent(event: AddDeviceScreenEvent) {
		when (event) {
			AddDeviceScreenEvent.OnRefreshDeviceList -> onRefreshDeviceList()
			AddDeviceScreenEvent.OnStartDeviceScan -> onStartScan()
			AddDeviceScreenEvent.OnStopDeviceScan -> onStopScan()
		}
	}


	private fun onRefreshDeviceList() = viewModelScope.launch {
		// if not scanning restart the scan
		if (!isScanning.value) {
			onStartScan()
			_isListRefreshing.update { true }
			_uiEvents.emit(UIEvents.ShowSnackBar("Restarting Scan"))
			_isListRefreshing.update { false }
		}
		// otherwise clear the list
		bleScanManager.onClearScanResults()
	}

	private fun onStartScan() {
		_bleScanJob = viewModelScope.launch {
			val result = bleScanManager.startScan()
			result.onFailure { err ->
				_uiEvents.emit(UIEvents.ShowSnackBar(err.message ?: "Some error"))
			}
		}
	}

	private fun onStopScan() = viewModelScope.launch {
		_bleScanJob?.cancel()
		_bleScanJob = null
		bleScanManager.stopScanning()
	}

	override fun onCleared() {
		// cancel the scan job when cleared
		_bleScanJob?.cancel()
		_bleScanJob = null
	}

}