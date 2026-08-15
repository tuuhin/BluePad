package com.sam.bluepad.data.compression

import com.sam.bluepad.domain.compression.ICompressionManager

expect class CompressionManagerImpl : ICompressionManager {

    override suspend fun compressBytes(bytes: ByteArray): ByteArray
    override suspend fun inflateBytes(bytes: ByteArray): ByteArray
}
