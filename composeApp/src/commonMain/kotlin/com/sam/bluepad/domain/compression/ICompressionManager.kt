package com.sam.bluepad.domain.compression

interface ICompressionManager {

    suspend fun compressBytes(bytes: ByteArray): ByteArray
    suspend fun inflateBytes(bytes: ByteArray): ByteArray
}
