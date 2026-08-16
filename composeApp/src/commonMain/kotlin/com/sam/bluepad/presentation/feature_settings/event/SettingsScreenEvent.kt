package com.sam.bluepad.presentation.feature_settings.event

import com.sam.bluepad.domain.compression.CompressionLevel

sealed interface SettingsScreenEvent {
    data class OnUpdateDeviceName(val newName: String) : SettingsScreenEvent
    data object OnToggleAppFont : SettingsScreenEvent
    data object UseDynamicColor : SettingsScreenEvent

    // sync events
    data class OnUpdatePayloadSize(val size: Int) : SettingsScreenEvent
    data class OnUpdateCompressionLevel(val level: CompressionLevel) : SettingsScreenEvent
}
