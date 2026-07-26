package com.sam.bluepad.presentation.feature_settings.event

import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.settings.models.UserAppSettingsModel

data class SettingsScreenState(
    val device: LocalDeviceInfoModel? = null,
    val platformOs: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    val appSettings: UserAppSettingsModel = UserAppSettingsModel(),
)
