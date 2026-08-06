package com.sam.bluepad.compression

import com.sam.bluepad.compression.model.CompressionAlgo

interface DataCompressor {

    fun isAlgoAvailable(algo: CompressionAlgo): Boolean

    suspend fun compress(bytes: ByteArray, algo: CompressionAlgo = CompressionAlgo.COMPRESS_LZ4): ByteArray
    suspend fun unCompress(bytes: ByteArray, algo: CompressionAlgo = CompressionAlgo.COMPRESS_LZ4): ByteArray
}
