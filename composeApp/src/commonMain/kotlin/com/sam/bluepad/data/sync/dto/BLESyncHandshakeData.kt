package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Serializable
@SerialName("_bshd")
sealed class BLESyncHandshakeData {

    @Serializable
    @SerialName("add")
    data class AdvertiseDeviceData(
        @ProtoNumber(1) val deviceId: Uuid,
        @ProtoNumber(2) val nonce: String,
        @ProtoNumber(3) val allowSync: Boolean = true
    ) : BLESyncHandshakeData()

    @Serializable
    @SerialName("ard")
    data class AdvertiseResponseData(
        @ProtoNumber(1) val receiverID: Uuid,
        @ProtoNumber(2) val senderID: Uuid,
        @ProtoNumber(3) val nonce: String,
    ) : BLESyncHandshakeData()

    @Serializable
    @SerialName("has")
    data class HandshakeACKSuccess(
        @ProtoNumber(2) val nonce: String,
    ) : BLESyncHandshakeData()

    @Serializable
    @SerialName("haf")
    data class HandshakeACKFailed(
        @ProtoNumber(1) val reason: BLEHandshakeFailedReason
    ) : BLESyncHandshakeData()


}