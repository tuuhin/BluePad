package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import com.sam.bluepad.data.datastore.serializers.UserAppSettingsKT
import com.sam.bluepad.domain.settings.UserAppSettingsProvider
import com.sam.bluepad.domain.settings.models.AppFontOption
import com.sam.bluepad.domain.settings.models.UserAppSettingsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class UserAppSettingsProviderImpl(
    private val dataStoreProvider: DataStoreProvider,
) : UserAppSettingsProvider {

    private val dataStore: DataStore<UserAppSettingsKT> by lazy {
        dataStoreProvider.provideSettingsDataStore(
            DataStoreUtils.APP_USER_SETTINGS_DATASTORE_FILE,
        )
    }

    override val settingsFlow: Flow<UserAppSettingsModel>
        get() = dataStore.data.map { it.toDomain() }
            .flowOn(Dispatchers.IO)

    override suspend fun toggleUseSystemFont() {
        dataStore.updateData { data -> data.copy(use_system_font = !data.use_system_font) }
    }

    private fun UserAppSettingsKT.toDomain() =
        UserAppSettingsModel(fontOption = if (use_system_font) AppFontOption.SYSTEM else AppFontOption.DEFAULT)
}
