package com.sam.bluepad.domain.use_cases

import kotlin.io.encoding.Base64

class BytesEncoder {

    fun encodeBytes(bytes: ByteArray): String {
        return Base64.encode(bytes)
    }
}