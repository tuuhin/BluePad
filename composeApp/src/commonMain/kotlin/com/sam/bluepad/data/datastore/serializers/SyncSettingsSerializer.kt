package com.sam.bluepad.data.datastore.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import com.sam.bluepad.data.proto.UserSyncSettingsProto
import com.sam.bluepad.domain.compression.CompressionLevel
import com.sam.bluepad.domain.settings.models.SyncSettingsModel
import kotlinx.io.IOException
import okio.BufferedSink
import okio.BufferedSource

internal typealias SyncSettingsKT = UserSyncSettingsProto

internal object SyncSettingsSerializer : OkioSerializer<UserSyncSettingsProto> {

    override val defaultValue: UserSyncSettingsProto = UserSyncSettingsProto(
        sync_compression_level = CompressionLevel.LEVEL_3.level,
        sync_chunk_size = SyncSettingsModel.MIN_SYNC_CHUNK_SIZE,
    )

    override suspend fun readFrom(source: BufferedSource): UserSyncSettingsProto {
        return try {
            UserSyncSettingsProto.ADAPTER.decode(source)
        } catch (e: IOException) {
            throw CorruptionException("FAILED TO READ THE FILE", e)
        }
    }

    override suspend fun writeTo(t: UserSyncSettingsProto, sink: BufferedSink) {
        UserSyncSettingsProto.ADAPTER.encode(sink, t)
    }
}
