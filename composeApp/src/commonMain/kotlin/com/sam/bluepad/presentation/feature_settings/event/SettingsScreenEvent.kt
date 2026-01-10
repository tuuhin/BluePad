package com.sam.bluepad.presentation.feature_settings.event

sealed interface SettingsScreenEvent {
	data class OnUpdateDeviceName(val name: String = "") : SettingsScreenEvent
}