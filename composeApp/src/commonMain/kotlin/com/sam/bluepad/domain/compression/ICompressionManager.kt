package com.sam.bluepad.domain.compression

interface ICompressionManager {

    suspend fun compressBytes(bytes: ByteArray, level: CompressionLevel = CompressionLevel.LEVEL_3): ByteArray
    suspend fun inflateBytes(bytes: ByteArray): ByteArray

}
