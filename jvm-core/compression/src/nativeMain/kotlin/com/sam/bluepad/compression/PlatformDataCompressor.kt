package com.sam.bluepad.compression

expect class PlatformDataCompressor {
    val isLz4Available: Boolean
    val isZStdAvailable: Boolean
    val isGzipAvailable: Boolean
    val isDeflateAvailable: Boolean

    fun compressWithLz4(bytes: ByteArray): ByteArray
    fun unCompressWithLz4(bytes: ByteArray): ByteArray

    fun compressWIthZStd(bytes: ByteArray): ByteArray
    fun unCompressWithZtd(bytes: ByteArray): ByteArray

    fun compressWithGzip(bytes: ByteArray): ByteArray
    fun unCompressWithGzip(bytes: ByteArray): ByteArray

    fun compressWithDeflate(bytes: ByteArray): ByteArray
    fun uncompressWithDeflate(bytes: ByteArray): ByteArray
}
