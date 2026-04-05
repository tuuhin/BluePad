package com.sam.bluepad.presentation.feature_settings

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.presentation.feature_settings.event.CurrentDeviceState
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewmodel(
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val platformInfoProvider: PlatformInfoProvider,
) : AppViewModel() {

    val localDeviceData = localDeviceProvider.readDeviceInfo
        .map { device ->
            CurrentDeviceState(
                deviceName = device.name,
                deviceId = device.deviceId.toHexDashString(),
                platformOS = platformInfoProvider.platformOS
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            null
        )


    private val _uiEvent = MutableSharedFlow<UIEvents>()
    override val uiEvent: SharedFlow<UIEvents>
        get() = _uiEvent

    fun onEvent(event: SettingsScreenEvent) {
        when (event) {
            is SettingsScreenEvent.OnUpdateDeviceName -> onUpdateDeviceName(event.newName)
        }
    }

    private fun onUpdateDeviceName(name: String) = viewModelScope.launch {
        localDeviceProvider.updateDeviceName(name)
        _uiEvent.emit(UIEvents.ShowSnackBar("Name updated"))
    }

}