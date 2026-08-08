package com.sam.bluepad.compression

import com.sam.bluepad.compression.model.CompressionAlgo
import com.sam.bluepad.compression.native.PlatformDataCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeDataCompressor : DataCompressor, AutoCloseable {

    private val compressor by lazy { PlatformDataCompressor() }

    override fun isAlgoAvailable(algo: CompressionAlgo): Boolean {
        return when (algo) {
            CompressionAlgo.COMPRESS_NONE -> true
            CompressionAlgo.COMPRESS_LZ4 -> compressor.isLz4Available
            CompressionAlgo.COMPRESS_ZSTD -> compressor.isZStdAvailable
            CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> compressor.isDeflateAvailable
            CompressionAlgo.COMPRESS_GZIP -> compressor.isGzipAvailable
        }
    }

    override suspend fun compress(bytes: ByteArray, algo: CompressionAlgo): ByteArray =
        withContext(Dispatchers.Default) {
            when (algo) {
                CompressionAlgo.COMPRESS_NONE -> bytes
                CompressionAlgo.COMPRESS_LZ4 -> compressor.compressWithLz4(bytes)
                CompressionAlgo.COMPRESS_ZSTD -> compressor.compressWIthZStd(bytes)
                CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> compressor.compressWithDeflate(bytes)
                CompressionAlgo.COMPRESS_GZIP -> compressor.compressWithGzip(bytes)
            }
        }

    override suspend fun unCompress(bytes: ByteArray, algo: CompressionAlgo): ByteArray =
        withContext(Dispatchers.Default) {
            when (algo) {
                CompressionAlgo.COMPRESS_NONE -> bytes
                CompressionAlgo.COMPRESS_LZ4 -> compressor.unCompressWithLz4(bytes)
                CompressionAlgo.COMPRESS_ZSTD -> compressor.unCompressWithZtd(bytes)
                CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> compressor.uncompressWithDeflate(bytes)
                CompressionAlgo.COMPRESS_GZIP -> compressor.unCompressWithGzip(bytes)
            }
        }

    override fun close() {
        compressor.close()
    }
}
