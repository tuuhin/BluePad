package com.sam.preferences.data.repository

import androidx.datastore.core.DataStore
import com.sam.bluepad.domain.repository.IUserAppSettingsRepository
import com.sam.bluepad.model.common.AppFontOption
import com.sam.bluepad.model.common.UserAppSettingsModel
import com.sam.preferences.data.datastore.DataStoreCommon
import com.sam.preferences.data.datastore.DataStoreProvider
import com.sam.preferences.data.datastore.UserAppSettingsKT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton

@Singleton(binds = [IUserAppSettingsRepository::class])
internal class UserAppSettingsRepo(
    private val dataStoreProvider: DataStoreProvider
) : IUserAppSettingsRepository {


    private val dataStore: DataStore<UserAppSettingsKT> by lazy {
        dataStoreProvider.provideSettingsDataStore(
            DataStoreCommon.APP_USER_SETTINGS_DATASTORE_FILE,
        )
    }

    override val settingsFlow: Flow<UserAppSettingsModel>
        get() = dataStore.data.map { it.toDomain() }
            .flowOn(Dispatchers.IO)

    override suspend fun toggleUseSystemFont() {
        dataStore.updateData { data -> data.copy(use_system_font = !data.use_system_font) }
    }

    override suspend fun toggleUseDynamicColor() {
        dataStore.updateData { data -> data.copy(use_dynamic_colors = !data.use_dynamic_colors) }
    }

    private fun UserAppSettingsKT.toDomain() =
        UserAppSettingsModel(
            fontOption = if (use_system_font) AppFontOption.SYSTEM else AppFontOption.DEFAULT,
            useDynamicColor = use_dynamic_colors,
        )
}
