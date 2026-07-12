package com.sam.bluepad

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppCommonViewModel(
    provider: BluetoothStateProvider,
    private val btEnableUseCase: BTEnableRequestProvider,
) : AppViewModel() {

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    val bluetoothState = provider.bluetoothStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = false,
    )

    fun onRequestEnableBT() = viewModelScope.launch {
        // don't consider this if bluetooth is already enabled
        if (bluetoothState.value) return@launch

        val result = btEnableUseCase.invoke()
        result.fold(
            onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Bluetooth enabled")) },
            onFailure = { err ->
                val message = err.message ?: "Unable to enable bluetooth"
                _uiEvents.emit(UIEvents.ShowSnackBar(message))
            },
        )
    }
}
