package com.sam.bluepad.model.common

data class UserAppSettingsModel(
    val fontOption: AppFontOption = AppFontOption.SYSTEM,
    val useDynamicColor: Boolean = false,
)
