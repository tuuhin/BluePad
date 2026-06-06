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
            subclass(
                BLESyncHandshakeData.AdvertiseDeviceData::class,
                BLESyncHandshakeData.AdvertiseDeviceData.serializer(),
            )
            subclass(
                BLESyncHandshakeData.AdvertiseResponseData::class,
                BLESyncHandshakeData.AdvertiseResponseData.serializer(),
            )
            subclass(
                BLESyncHandshakeData.HandshakeACKSuccess::class,
                BLESyncHandshakeData.HandshakeACKSuccess.serializer(),
            )
            subclass(
                BLESyncHandshakeData.HandshakeACKFailed::class,
                BLESyncHandshakeData.HandshakeACKFailed.serializer(),
            )
        }
    }

    private fun SerializersModuleBuilder.bleSyncPacketSerializer() {
        polymorphic(BLESyncSession::class) {
            subclass(BLESyncSession.SyncSessionStart::class, BLESyncSession.SyncSessionStart.serializer())
            subclass(BLESyncSession.BLESyncDataPacket::class, BLESyncSession.BLESyncDataPacket.serializer())
            subclass(BLESyncSession.BLESyncDataAck::class, BLESyncSession.BLESyncDataAck.serializer())
            subclass(BLESyncSession.SyncSessionSuccessful::class, BLESyncSession.SyncSessionSuccessful.serializer())
            subclass(
                BLESyncSession.SyncSessionSuccessfulAck::class,
                BLESyncSession.SyncSessionSuccessfulAck.serializer(),
            )
            subclass(BLESyncSession.SyncSessionFailed::class, BLESyncSession.SyncSessionFailed.serializer())
            subclass(BLESyncSession.SyncPacketTransition::class, BLESyncSession.SyncPacketTransition.serializer())
            subclass(BLESyncSession.BLESyncDataPacketEnd::class, BLESyncSession.BLESyncDataPacketEnd.serializer())
            subclass(BLESyncSession.SyncPacketProcessing::class, BLESyncSession.SyncPacketProcessing.serializer())
            subclass(BLESyncSession.SyncSessionStartAck::class, BLESyncSession.SyncSessionStartAck.serializer())
        }
    }

    private fun SerializersModuleBuilder.createSyncDataFrameSerializer() {
        polymorphic(SyncPayloadSequence::class) {
            subclass(SyncPayloadSequence.Content::class, SyncPayloadSequence.Content.serializer())
            subclass(SyncPayloadSequence.MetaData::class, SyncPayloadSequence.MetaData.serializer())
        }
    }

    private fun SerializersModuleBuilder.createSYncDataSerializer() {
        polymorphic(SyncDataFrame::class) {
            subclass(SyncDataFrame.Metadata::class, SyncDataFrame.Metadata.serializer())
            subclass(SyncDataFrame.Content::class, SyncDataFrame.Content.serializer())
        }
    }

    private fun SerializersModuleBuilder.createDiffSerializer() {
        polymorphic(SyncDiffChangesDTO::class) {
            subclass(SyncDiffChangesDTO.InsertChange::class, SyncDiffChangesDTO.InsertChange.serializer())
            subclass(SyncDiffChangesDTO.DeleteChange::class, SyncDiffChangesDTO.DeleteChange.serializer())
            subclass(SyncDiffChangesDTO.UpdateChange::class, SyncDiffChangesDTO.UpdateChange.serializer())
        }
    }
}
