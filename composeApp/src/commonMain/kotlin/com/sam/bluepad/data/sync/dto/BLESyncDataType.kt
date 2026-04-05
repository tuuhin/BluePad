package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("__st")
enum class BLESyncDataType {
    @ProtoNumber(1)
    METADATA,

    @ProtoNumber(2)
    CONTENT_REQUEST,

    @ProtoNumber(3)
    CONTENT,
}
