package com.sam.bluepad.presentation.feature_settings.event

import com.sam.bluepad.domain.models.DevicePlatformOS

data class CurrentDeviceState(
    val deviceName: String,
    val deviceId: String,
    val platformOS: DevicePlatformOS = DevicePlatformOS.UNKNOWN
)
