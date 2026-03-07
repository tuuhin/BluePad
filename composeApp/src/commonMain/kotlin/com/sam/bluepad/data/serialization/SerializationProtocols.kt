package com.sam.bluepad.data.serialization

import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf

object SerializationProtocols {

    val protobuf = ProtoBuf {
        encodeDefaults = true
        serializersModule = SerializersModule {
            // ble pre sync data
            polymorphic(BLESyncHandshakeData::class) {
                subclass(
                    BLESyncHandshakeData.AdvertiseDeviceData::class,
                    BLESyncHandshakeData.AdvertiseDeviceData.serializer()
                )
                subclass(
                    BLESyncHandshakeData.AdvertiseResponseData::class,
                    BLESyncHandshakeData.AdvertiseResponseData.serializer()
                )
                subclass(
                    BLESyncHandshakeData.HandshakeACKSuccess::class,
                    BLESyncHandshakeData.HandshakeACKSuccess.serializer()
                )
                subclass(
                    BLESyncHandshakeData.HandshakeACKFailed::class,
                    BLESyncHandshakeData.HandshakeACKFailed.serializer()
                )
            }

            // ble sync exchange data
            polymorphic(BLESyncSession::class) {
                subclass(
                    BLESyncSession.SyncSessionStart::class,
                    BLESyncSession.SyncSessionStart.serializer()
                )
                subclass(
                    BLESyncSession.BLESyncDataPacket::class,
                    BLESyncSession.BLESyncDataPacket.serializer()
                )
                subclass(
                    BLESyncSession.BLESyncDataAck::class,
                    BLESyncSession.BLESyncDataAck.serializer()
                )
                subclass(
                    BLESyncSession.SyncSessionCompleted::class,
                    BLESyncSession.SyncSessionCompleted.serializer()
                )
                subclass(
                    BLESyncSession.SyncSessionFailed::class,
                    BLESyncSession.SyncSessionFailed.serializer()
                )
                subclass(
                    BLESyncSession.SyncPacketTransition::class,
                    BLESyncSession.SyncPacketTransition.serializer()
                )
                subclass(
                    BLESyncSession.SyncSessionStartAck::class,
                    BLESyncSession.SyncSessionStartAck.serializer()
                )
            }

            // sync packet data
            polymorphic(SyncDataFrame::class) {
                subclass(
                    SyncDataFrame.Metadata::class,
                    SyncDataFrame.Metadata.serializer()
                )
                subclass(
                    SyncDataFrame.Content::class,
                    SyncDataFrame.Content.serializer()
                )
            }
            // sync data frame
            polymorphic(SyncPayloadSequence::class) {
                subclass(
                    SyncPayloadSequence.Content::class,
                    SyncPayloadSequence.Content.serializer()
                )
                subclass(
                    SyncPayloadSequence.MetaData::class,
                    SyncPayloadSequence.MetaData.serializer()
                )
            }
        }
    }
}