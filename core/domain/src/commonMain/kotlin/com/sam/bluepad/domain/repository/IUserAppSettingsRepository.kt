package com.sam.bluepad.domain.repository

import com.sam.bluepad.model.common.UserAppSettingsModel
import kotlinx.coroutines.flow.Flow

interface IUserAppSettingsRepository {


    val settingsFlow: Flow<UserAppSettingsModel>

    suspend fun toggleUseSystemFont()

    suspend fun toggleUseDynamicColor()
}
