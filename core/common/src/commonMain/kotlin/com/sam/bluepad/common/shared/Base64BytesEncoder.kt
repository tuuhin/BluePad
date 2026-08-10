package com.sam.bluepad.common.shared

import org.koin.core.annotation.Factory
import kotlin.io.encoding.Base64

@Factory(binds = [IBytesEncoder::class])
class Base64BytesEncoder : IBytesEncoder {


    override fun encodeBytes(bytes: ByteArray): String {
        return Base64.encode(bytes)
    }

    override fun decodeBytes(data: String): ByteArray {
        return Base64.decode(data)
    }
}
