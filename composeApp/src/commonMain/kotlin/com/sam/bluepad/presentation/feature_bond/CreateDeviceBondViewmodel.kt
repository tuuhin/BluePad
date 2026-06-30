package com.sam.bluepad.presentation.feature_bond

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.bluetooth.BTDeviceBondManager
import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.bluetooth.models.BTDeviceBondInfo
import com.sam.bluepad.presentation.feature_bond.state.CreateBondDialogEvents
import com.sam.bluepad.presentation.feature_bond.state.CreateBondDialogState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CreateDeviceBondViewmodel(
    private val address: String,
    private val btDeviceBondManager: BTDeviceBondManager,
) : AppViewModel() {

    private val _bondDialogState = MutableStateFlow(CreateBondDialogState())
    val bondDialogState = _bondDialogState
        .map { state -> state.copy(canShowConfirmPinInDialog = btDeviceBondManager.canShowConfirmPinDialog) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreateBondDialogState(),
        )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private var _btDeviceBondJob: Job? = null

    fun onEvent(event: CreateBondDialogEvents) {
        when (event) {
            CreateBondDialogEvents.OnAcceptConfirmPin -> onAcceptPinConfirm()
            CreateBondDialogEvents.OnCancelBondForDevice -> onDenyBond()
            CreateBondDialogEvents.OnRequestBondForDevice -> onRequestBond(address)
        }
    }

    private fun onAcceptPinConfirm() = viewModelScope.launch {
        val confirmPin = _bondDialogState.value.confirmPin ?: run {
            _bondDialogState.update { state -> state.copy(error = "Unable to read confirm pin") }
            return@launch
        }
        _bondDialogState.update { state -> state.copy(isPrimaryActionEnabled = false) }

        // only mark the primary action enabled if something went wrong
        // as if the deferral is completed we still need to wait for the confirmation pin
        // from the other device
        btDeviceBondManager.acceptBondConfirmationPin(confirmPin)
            .onFailure { err ->
                val message = err.message ?: "some issues to bond the device"
                _bondDialogState.update { state ->
                    state.copy(error = message, isPrimaryActionEnabled = true)
                }
            }
    }

    private fun onDenyBond() = viewModelScope.launch {
        _uiEvents.emit(UIEvents.PopScreen)
    }

    private fun onRequestBond(deviceAddress: String) {
        _btDeviceBondJob?.cancel()
        _btDeviceBondJob = btDeviceBondManager.requestBond(deviceAddress)
            .onStart { _bondDialogState.update { state -> state.copy(isPrimaryActionEnabled = false, error = null) } }
            .onCompletion { _bondDialogState.update { state -> state.copy(isPrimaryActionEnabled = true) } }
            .catch { err ->
                val errMessage = err.message ?: "Cannot perform bond"
                _bondDialogState.update { state -> state.copy(error = errMessage) }
            }
            .onEach(::onHandleBondInfo)
            .launchIn(viewModelScope)
    }


    private suspend fun onHandleBondInfo(bondInfo: BTDeviceBondInfo) {
        when (bondInfo) {
            is BTDeviceBondInfo.BondState -> {
                when (bondInfo.state) {
                    BTDeviceBondState.BONDED -> {
                        _uiEvents.emit(UIEvents.ShowToast("Device bonded"))
                        onDenyBond()
                    }

                    BTDeviceBondState.NOT_BONDED -> _bondDialogState.update { state ->
                        state.copy(error = "Unable to perform bond", isPrimaryActionEnabled = true, confirmPin = null)
                    }

                    else -> {}
                }

            }

            is BTDeviceBondInfo.ConfirmPin -> _bondDialogState.update { state ->
                state.copy(confirmPin = bondInfo.string, isPrimaryActionEnabled = true, error = null)
            }
        }
    }

    override fun onCleared() {
        _btDeviceBondJob?.cancel()
        _btDeviceBondJob = null
    }
}
