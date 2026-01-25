package com.sam.bluepad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppCommonViewModel(
	provider: BluetoothStateProvider
) : ViewModel() {

	val bluetoothState = provider.bluetoothStatusFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000L),
		initialValue = false
	)
}