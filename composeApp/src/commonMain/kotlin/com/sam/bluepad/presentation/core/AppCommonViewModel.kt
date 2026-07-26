package com.sam.bluepad.presentation.core

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppCommonViewModel(
    provider: BluetoothStateProvider,
    private val btRequestEnableProvider: BTEnableRequestProvider,
) : AppViewModel() {

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private val _canActiveBT = btRequestEnableProvider.canRequestBTActive
    private val _canOpenSettings = btRequestEnableProvider.canOpenSettingsToActivateBT

    val bluetoothState = provider.bluetoothStatusFlow
        .map {
            AppBluetoothState(
                isBTActive = it,
                canOpenBTSettings = _canOpenSettings,
                canRequestBTActive = _canActiveBT,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            // considering bluetooth is enabled as we capture mainly for not enabled case
            initialValue = AppBluetoothState(isBTActive = true),
        )

    fun onRequestEnableBT() = viewModelScope.launch {
        // Skip this if bluetooth is already enabled
        if (bluetoothState.value.isBTActive) return@launch

        val result = btRequestEnableProvider.requestActive()
        result.fold(
            onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Bluetooth enabled")) },
            onFailure = { err ->
                val message = err.message ?: "Unable to enable bluetooth"
                _uiEvents.emit(UIEvents.ShowSnackBar(message))
            },
        )
    }

    fun onOpenAppSettings() = viewModelScope.launch {
        // Skip this if bluetooth is already enabled , being not needed in the context for this app
        if (bluetoothState.value.isBTActive) return@launch
        btRequestEnableProvider.onOpenSettings()
    }
}
