package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("__sfr")
enum class BLESyncFailedReason {

    @ProtoNumber(1)
    TAMPERED_DATA;
}