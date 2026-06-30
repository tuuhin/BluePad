package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import com.sam.bluepad.domain.bluetooth.BTDeviceBondManager
import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.exceptions.LocationPermissionException
import com.sam.bluepad.presentation.feature_devices.events.AddDeviceScreenEvent
import com.sam.bluepad.presentation.feature_devices.events.ScanDevicesNavEvent
import com.sam.bluepad.presentation.feature_devices.state.AddDeviceScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEScanDevicesViewModel(
    private val bleScanManager: BLEDiscoveryManager,
    private val permissionManger: PermissionsController,
    private val btDeviceBondManager: BTDeviceBondManager,
) : AppViewModel() {

    private val _isListRefreshing = MutableStateFlow(false)

    val state = combine(
        _isListRefreshing,
        bleScanManager.isScanning,
        bleScanManager.scanResults,
    ) { isRefreshing, isScanning, scanResults ->
        AddDeviceScreenState(
            isListRefreshing = isRefreshing,
            isScanning = isScanning,
            peers = scanResults.toImmutableList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AddDeviceScreenState(),
    )

    private val _uiEvents = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvents

    private val _navEvent =
        MutableSharedFlow<ScanDevicesNavEvent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val navEvent = _navEvent.asSharedFlow()

    private var _bleScanJob: Job? = null

    fun onEvent(event: AddDeviceScreenEvent) {
        when (event) {
            AddDeviceScreenEvent.OnRefreshDeviceList -> onRefreshDeviceList()
            AddDeviceScreenEvent.OnStartDeviceScan -> onStartScan()
            AddDeviceScreenEvent.OnStopDeviceScan -> onStopScan()
            is AddDeviceScreenEvent.CheckBondStateForDevice -> onCheckBondStateOrConnect(event.device)
        }
    }


    private fun onCheckBondStateOrConnect(device: BLEPeerDevice) = viewModelScope.launch {

        if (!btDeviceBondManager.isFeatureAvailable) {
            // if feature is missing navigate directly to connect
            _navEvent.tryEmit(ScanDevicesNavEvent.NavigateToConnect(device))
            return@launch
        }

        val state = btDeviceBondManager.checkBondState(device.deviceAddress)
        // otherwise if are bonded navigate to connect directly otherwise to bond dialog
        state.fold(
            onSuccess = { bondState ->
                when (bondState) {
                    BTDeviceBondState.BONDED -> _navEvent.tryEmit(ScanDevicesNavEvent.NavigateToConnect(device))
                    BTDeviceBondState.NOT_BONDED -> _navEvent.tryEmit(ScanDevicesNavEvent.NavigateToCreateBond(device))
                    BTDeviceBondState.BONDING -> _uiEvents.emit(UIEvents.ShowSnackBar("Please wait a second"))
                }
            },
            onFailure = { err ->
                val message = err.message ?: "Unable to fetch bond state"
                _uiEvents.emit(UIEvents.ShowSnackBar(message))
            },
        )
    }

    private fun onRefreshDeviceList() = viewModelScope.launch {
        val isScanning = state.value.isScanning
        // if not scanning restart the scan
        if (!isScanning) {
            onStartScan()
            _isListRefreshing.update { true }
            _uiEvents.emit(UIEvents.ShowSnackBar("Restarting Scan"))
            _isListRefreshing.update { false }
        }
        // clear the internal scan results cache so we get a fresh list
        bleScanManager.onClearScanResults()
    }

    private fun onStartScan() {
        _bleScanJob = viewModelScope.launch {
            val result = bleScanManager.startScan()
            result.fold(
                onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Scan started")) },
                onFailure = { err ->
                    when (err) {
                        is LocationPermissionException -> {
                            val message = "Allow location permission from settings"
                            handlePermission(Permission.LOCATION, message)
                        }

                        is BluetoothPermissionException -> {
                            val message = "Bluetooth scan permission required "
                            handlePermission(Permission.BLUETOOTH_LE, message)
                        }

                        else -> {
                            val message = err.message ?: "Some error occurred"
                            _uiEvents.emit(UIEvents.ShowSnackBar(message))
                        }
                    }
                },
            )
        }
    }

    private fun handlePermission(
        permission: Permission,
        message: String? = null,
        onSuccess: () -> Unit = {}
    ) = viewModelScope.launch {
        val state = permissionManger.getPermissionState(permission)
        val settingsMessage = message ?: "Open settings and allow :$permission permission"

        when (state) {
            PermissionState.NotGranted, PermissionState.NotDetermined, PermissionState.Denied -> {
                try {
                    permissionManger.providePermission(permission)
                    onSuccess()
                } catch (_: DeniedException) {
                    _uiEvents.emit(UIEvents.ShowSnackBar("Permission denied"))
                } catch (_: DeniedAlwaysException) {
                    _uiEvents.emit(
                        UIEvents.ShowSnackBarWithActions(
                            settingsMessage,
                            action = { permissionManger.openAppSettings() },
                            actionText = "Settings",
                        ),
                    )
                } catch (_: RequestCanceledException) {
                }
            }

            PermissionState.DeniedAlways -> _uiEvents.emit(
                UIEvents.ShowSnackBarWithActions(
                    settingsMessage,
                    action = { permissionManger.openAppSettings() },
                    actionText = "Settings",
                ),
            )

            PermissionState.Granted -> onSuccess()
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
