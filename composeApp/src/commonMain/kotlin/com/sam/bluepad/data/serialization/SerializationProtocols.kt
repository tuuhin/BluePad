package com.sam.bluepad.data.serialization

import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync_diff.dto.SyncDiffChangesDTO
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf

object SerializationProtocols {

    val protobuf = ProtoBuf {
        encodeDefaults = true
        serializersModule = SerializersModule {
            bleHandshakeSerializer()
            bleSyncPacketSerializer()
            createSyncDataFrameSerializer()
            createSYncDataSerializer()
            createDiffSerializer()
        }
    }

    private fun SerializersModuleBuilder.bleHandshakeSerializer() {
        // ble pre sync data
        polymorphic(BLESyncHandshakeData::class) {
            subclassesOfSealed<BLESyncHandshakeData>()
        }
    }

    private fun SerializersModuleBuilder.bleSyncPacketSerializer() {
        polymorphic(BLESyncSession::class) {
            subclassesOfSealed<BLESyncSession>()
        }
    }

    private fun SerializersModuleBuilder.createSyncDataFrameSerializer() {
        polymorphic(SyncPayloadSequence::class) {
            subclassesOfSealed<SyncPayloadSequence>()
        }
    }

    private fun SerializersModuleBuilder.createSYncDataSerializer() {
        polymorphic(SyncDataFrame::class) {
            subclassesOfSealed<SyncDataFrame>()
        }
    }

    private fun SerializersModuleBuilder.createDiffSerializer() {
        polymorphic(SyncDiffChangesDTO::class) {
            subclassesOfSealed<SyncDiffChangesDTO>()
        }
    }
}
