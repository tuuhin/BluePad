package com.sam.bluepad.domain.settings

import com.sam.bluepad.domain.settings.models.UserAppSettingsModel
import kotlinx.coroutines.flow.Flow

interface UserAppSettingsProvider {

    val settingsFlow: Flow<UserAppSettingsModel>

    suspend fun toggleUseSystemFont()

    suspend fun toggleUseDynamicColor()
}
