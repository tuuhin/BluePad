package com.sam.bluepad.data.datastore.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import com.sam.bluepad.data.proto.UserAppSettingsProto
import kotlinx.io.IOException
import okio.BufferedSink
import okio.BufferedSource

internal typealias UserAppSettingsKT = UserAppSettingsProto

internal object AppSettingsSerializer : OkioSerializer<UserAppSettingsKT> {

    override val defaultValue: UserAppSettingsKT =
        UserAppSettingsKT(use_system_font = false, use_dynamic_colors = false)

    override suspend fun readFrom(source: BufferedSource): UserAppSettingsKT {
        try {
            return UserAppSettingsKT.ADAPTER.decode(source)
        } catch (e: IOException) {
            throw CorruptionException("FAILED TO READ THE FILE", e)
        }
    }

    override suspend fun writeTo(t: UserAppSettingsKT, sink: BufferedSink) {
        UserAppSettingsKT.ADAPTER.encode(sink, t)
    }
}
