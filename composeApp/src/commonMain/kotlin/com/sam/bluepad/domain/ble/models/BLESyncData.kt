package com.sam.bluepad.domain.ble.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Serializable
sealed class BLESyncData {

    @Serializable
    data class BLEAdvertiseData(
        @ProtoNumber(1) val deviceId: Uuid,
        @ProtoNumber(2) val nonce: String? = null,
        @ProtoNumber(3) val allowSync: Boolean = true
    ) : BLESyncData()

    @Serializable
    data class BLEAdvertiseResponse(
        @ProtoNumber(1) val receiverDeviceId: Uuid,
        @ProtoNumber(2) val currentDeviceId: Uuid,
        @ProtoNumber(3) val nonce: String? = null
    ) : BLESyncData()

    @Serializable
    data class BLESyncAcknowledgement(
        @ProtoNumber(1) val serverID: Uuid,
        @ProtoNumber(2) val nonce: String? = null,
    ) : BLESyncData()
}