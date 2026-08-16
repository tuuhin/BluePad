package com.sam.bluepad.domain.compression

interface ICompressionManager {

    suspend fun compressBytes(bytes: ByteArray): ByteArray
    suspend fun inflateBytes(bytes: ByteArray): ByteArray

    companion object {
        const val COMPRESSION_LEVEL = 10
    }
}
