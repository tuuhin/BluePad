package com.sam.bluepad.common.shared

interface IBytesEncoder {


    fun encodeBytes(bytes: ByteArray): String

    fun decodeBytes(data: String): ByteArray
}
