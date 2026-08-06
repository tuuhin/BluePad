package com.sam.bluepad.compression

import com.sam.bluepad.compression.model.CompressionAlgo
import com.sam.bluepad.compression.native.PlatformDataCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeDataCompressor : DataCompressor {

    override fun isAlgoAvailable(algo: CompressionAlgo): Boolean {
        return when (algo) {
            CompressionAlgo.COMPRESS_NONE -> true
            CompressionAlgo.COMPRESS_LZ4 -> PlatformDataCompressor().use { it.isLz4Available }
            CompressionAlgo.COMPRESS_ZSTD -> PlatformDataCompressor().use { it.isZStdAvailable }
            CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> PlatformDataCompressor().use { it.isDeflateAvailable }
            CompressionAlgo.COMPRESS_GZIP -> PlatformDataCompressor().use { it.isGzipAvailable }
        }
    }

    override suspend fun compress(bytes: ByteArray, algo: CompressionAlgo): ByteArray =
        withContext(Dispatchers.Default) {
            val compressor = PlatformDataCompressor()
            compressor.use { compressor ->
                when (algo) {
                    CompressionAlgo.COMPRESS_NONE -> bytes
                    CompressionAlgo.COMPRESS_LZ4 -> compressor.compressWithLz4(bytes)
                    CompressionAlgo.COMPRESS_ZSTD -> compressor.compressWIthZStd(bytes)
                    CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> compressor.compressWithDeflate(bytes)
                    CompressionAlgo.COMPRESS_GZIP -> compressor.compressWithGzip(bytes)
                }
            }
        }

    override suspend fun unCompress(bytes: ByteArray, algo: CompressionAlgo): ByteArray =
        withContext(Dispatchers.Default) {
            val compressor = PlatformDataCompressor()
            compressor.use { compressor ->
                when (algo) {
                    CompressionAlgo.COMPRESS_NONE -> bytes
                    CompressionAlgo.COMPRESS_LZ4 -> compressor.unCompressWithLz4(bytes)
                    CompressionAlgo.COMPRESS_ZSTD -> compressor.unCompressWithZtd(bytes)
                    CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> compressor.uncompressWithDeflate(bytes)
                    CompressionAlgo.COMPRESS_GZIP -> compressor.unCompressWithGzip(bytes)
                }
            }
        }
}
