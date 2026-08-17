package com.sam.bluepad.presentation.feature_settings

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.settings.SyncSettingsProvider
import com.sam.bluepad.domain.settings.UserAppSettingsProvider
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewmodel(
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val platformInfoProvider: PlatformInfoProvider,
    private val settingsProvider: UserAppSettingsProvider,
    private val syncSettings: SyncSettingsProvider,
) : AppViewModel() {

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded = _isSettingsLoaded.asStateFlow()

    val state = combine(
        localDeviceProvider.readDeviceInfo,
        settingsProvider.settingsFlow,
        syncSettings.settingsFlow,
    ) { device, settings, syncSettings ->
        SettingsScreenState(
            device = device,
            platformOs = platformInfoProvider.platformOS,
            appSettings = settings,
            syncSettings = syncSettings,
        )
    }.onEach { _isSettingsLoaded.update { true } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(4_000L),
            initialValue = SettingsScreenState(),
        )

    override val uiEvent: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    fun onEvent(event: SettingsScreenEvent) {
        when (event) {
            is SettingsScreenEvent.OnUpdateDeviceName -> onUpdateDeviceName(event.newName)
            SettingsScreenEvent.OnToggleAppFont -> viewModelScope.launch { settingsProvider.toggleUseSystemFont() }
            SettingsScreenEvent.UseDynamicColor -> viewModelScope.launch { settingsProvider.toggleUseDynamicColor() }
            is SettingsScreenEvent.OnUpdatePayloadSize -> viewModelScope.launch { syncSettings.updatePayloadSize(event.size) }
            is SettingsScreenEvent.OnUpdateCompressionLevel -> viewModelScope.launch {
                syncSettings.updateCompressionLevel(event.level)
            }
        }
    }

    private fun onUpdateDeviceName(name: String) = viewModelScope.launch {
        localDeviceProvider.updateDeviceName(name)
        uiEvent.emit(UIEvents.ShowSnackBar("Name updated"))
    }
}
