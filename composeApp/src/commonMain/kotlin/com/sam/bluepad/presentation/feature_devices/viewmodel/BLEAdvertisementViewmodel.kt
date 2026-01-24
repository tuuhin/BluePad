package com.sam.bluepad.presentation.feature_devices.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.mappers.toExternalDevice
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_devices.events.AdvertisementScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEAdvertisementViewmodel(
	private val bleAdvertisement: BLEAdvertisementManager,
	private val repository: ExternalDevicesRepository,
	private val permissionManger: PermissionsController,
) : AppViewModel() {

	private val _errorMessage = MutableStateFlow<String?>(null)
	val errorMessage = _errorMessage.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	val isAdvertisementRunning = bleAdvertisement.isRunning
		.onStart { handleIncomingConnections() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	fun onEvent(event: AdvertisementScreenEvent) {
		when (event) {
			AdvertisementScreenEvent.OnStartAdvertise -> onStart()
			AdvertisementScreenEvent.OnStopAdvertise -> bleAdvertisement.stopAdvertising()
		}
	}

	private fun onStart() = viewModelScope.launch {
		val result = bleAdvertisement.startAdvertising()

		result.fold(
			onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Advertisement started")) },
			onFailure = { err ->
				if (err is BluetoothPermissionException) {
					val message = "Bluetooth le permission missing"
					handlePermission(Permission.BLUETOOTH_LE, message)
				}
				val message = err.message ?: "Some error occurred"
				_errorMessage.update { message }
			},
		)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private fun handleIncomingConnections() = bleAdvertisement.peerSaveDevices
		.flatMapConcat { peers ->
			val devices = peers.map { data -> data.toExternalDevice() }
			repository.saveOrUpdateDevices(devices)
		}.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Unable to save device"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> {
					val message = "${res.data.size} devices updated"
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				else -> {}
			}
		}.launchIn(viewModelScope)


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
							actionText = "Settings"
						)
					)
				} catch (_: RequestCanceledException) {
				}
			}

			PermissionState.DeniedAlways -> _uiEvents.emit(
				UIEvents.ShowSnackBarWithActions(
					settingsMessage,
					action = { permissionManger.openAppSettings() },
					actionText = "Settings"
				)
			)

			PermissionState.Granted -> onSuccess()
		}
	}

	override fun onCleared() {
		bleAdvertisement.cleanUp()
	}
}