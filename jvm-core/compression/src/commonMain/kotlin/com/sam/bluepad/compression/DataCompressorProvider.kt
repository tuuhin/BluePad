package com.sam.bluepad.compression

interface DataCompressorProvider {
    suspend fun compress(bytes: ByteArray): ByteArray
    suspend fun unCompress(bytes: ByteArray): ByteArray
}
