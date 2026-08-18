package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import com.sam.bluepad.data.datastore.serializers.SyncSettingsKT
import com.sam.bluepad.data.datastore.serializers.SyncSettingsSerializer
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.compression.CompressionLevel
import com.sam.bluepad.domain.settings.SyncSettingsProvider
import com.sam.bluepad.domain.settings.models.SyncSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SyncSettingsProviderImpl(
    private val provider: DataStoreProvider,
    private val dispatchers: PlatformDispatcherProvider,
) : SyncSettingsProvider {

    private val dataStore: DataStore<SyncSettingsKT> by lazy {
        provider.provideSettingsDataStore(
            serializer = SyncSettingsSerializer,
            fileName = DataStoreUtils.APP_SYNC_SETTINGS_DATASTORE_FILE,
        )
    }

    override val settingsFlow: Flow<SyncSettingsModel> = dataStore.data.map { it.toDomain() }
        .flowOn(dispatchers.io)

    override suspend fun settings(): SyncSettingsModel {
        return withContext(dispatchers.io) {
            settingsFlow.first()
        }
    }

    override suspend fun updateCompressionLevel(level: CompressionLevel) {
        dataStore.updateData { data ->
            data.copy(sync_compression_level = level.level)
        }
    }

    override suspend fun updatePayloadSize(size: Int) {
        dataStore.updateData { data ->
            data.copy(sync_chunk_size = size.coerceIn(SyncSettingsModel.MIN_SYNC_CHUNK_SIZE..SyncSettingsModel.MAX_SYNC_CHUNK_SIZE))
        }
    }

    fun SyncSettingsKT.toDomain(): SyncSettingsModel {
        val level = CompressionLevel.entries.find { it.level == sync_compression_level } ?: CompressionLevel.LEVEL_3
        val payloadSize =
            sync_chunk_size.coerceIn(SyncSettingsModel.MIN_SYNC_CHUNK_SIZE..SyncSettingsModel.MAX_SYNC_CHUNK_SIZE)
        return SyncSettingsModel(syncCompressionLevel = level, payloadSize)
    }
}
