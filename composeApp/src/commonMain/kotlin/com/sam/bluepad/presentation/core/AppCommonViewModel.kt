package com.sam.bluepad.presentation.core

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.platform.PlatformDeviceInfo
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppCommonViewModel(
    provider: BluetoothStateProvider,
    private val platformReader: IPlatformInfoReader,
    private val btRequestEnableProvider: BTEnableRequestProvider,
) : AppViewModel() {


    private val _canActiveBT = btRequestEnableProvider.canRequestBTActive
    private val _canOpenSettings = btRequestEnableProvider.canOpenSettingsToActivateBT

    private val _platformDetails = MutableStateFlow<PlatformDeviceInfo?>(null)
    val platformDetails = _platformDetails
        .filterNotNull()
        .onStart { readPlatform() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(10_000L),
            initialValue = PlatformDeviceInfo(),
        )

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

    override val uiEvent: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    fun onRequestEnableBT() = viewModelScope.launch {
        // Skip this if bluetooth is already enabled
        if (bluetoothState.value.isBTActive) return@launch

        val result = btRequestEnableProvider.requestActive()
        result.fold(
            onSuccess = { uiEvent.emit(UIEvents.ShowToast("Bluetooth enabled")) },
            onFailure = { err ->
                val message = err.message ?: "Unable to enable bluetooth"
                uiEvent.emit(UIEvents.ShowSnackBar(message))
            },
        )
    }

    fun onOpenAppSettings() = viewModelScope.launch {
        // Skip this if bluetooth is already enabled , being not needed in the context for this app
        if (bluetoothState.value.isBTActive) return@launch
        btRequestEnableProvider.onOpenSettings()
    }

    private fun readPlatform() = viewModelScope.launch {
        val result = platformReader.readPlatform()
        result.fold(onSuccess = { info -> _platformDetails.update { info } }, onFailure = {})
    }
}
