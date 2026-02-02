package com.sam.bluepad.data.serialization

import com.sam.bluepad.domain.ble.models.BLESyncData
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf

object SerializationProtocols {

    val protobuf = ProtoBuf {
        serializersModule = SerializersModule {
            polymorphic(BLESyncData::class) {
                subclass(
                    BLESyncData.BLEAdvertiseData::class,
                    BLESyncData.BLEAdvertiseData.serializer()
                )
                subclass(
                    BLESyncData.BLEAdvertiseResponse::class,
                    BLESyncData.BLEAdvertiseResponse.serializer()
                )
                subclass(
                    BLESyncData.BLESyncACKSuccess::class,
                    BLESyncData.BLESyncACKSuccess.serializer()
                )
                subclass(
                    BLESyncData.BLESyncACKFailed::class,
                    BLESyncData.BLESyncACKFailed.serializer()
                )
            }
        }
    }
}