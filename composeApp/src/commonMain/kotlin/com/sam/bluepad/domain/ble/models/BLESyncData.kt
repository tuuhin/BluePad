package com.sam.bluepad.domain.ble.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Serializable
sealed class BLESyncData {

    @Serializable
    data class BLEAdvertiseData(
        @ProtoNumber(1) val deviceId: Uuid,
        @ProtoNumber(2) val nonce: String,
        @ProtoNumber(3) val allowSync: Boolean = true
    ) : BLESyncData()

    @Serializable
    data class BLEAdvertiseResponse(
        @ProtoNumber(1) val receiverID: Uuid,
        @ProtoNumber(2) val senderID: Uuid,
        @ProtoNumber(3) val nonce: String,
    ) : BLESyncData()

    @Serializable
    data class BLESyncACKSuccess(
        @ProtoNumber(1) val serverID: Uuid,
        @ProtoNumber(2) val nonce: String,
        @ProtoNumber(3) val deviceAddress: String
    ) : BLESyncData()

    @Serializable
    data class BLESyncACKFailed(
        @ProtoNumber(1) val reason: BLESyncACKFailedReason
    ) : BLESyncData()
}