package com.sam.bluepad.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

class InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("InstantSerializerMillis", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant {
        val millis = decoder.decodeLong()
        return Instant.fromEpochMilliseconds(millis)
    }
}