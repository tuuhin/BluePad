package com.sam.bluepad.compression

import com.sam.bluepad.compression.algos.JVMDefalteCmpProvider
import com.sam.bluepad.compression.algos.JVMGzipCmpProvider
import com.sam.bluepad.compression.algos.JvmLz4CmpProvider
import com.sam.bluepad.compression.algos.JvmZTDCmpProvider
import com.sam.bluepad.compression.model.CompressionAlgo

class JVMDataCompressor : DataCompressor, AutoCloseable {

    private val defalte: JVMDefalteCmpProvider by lazy { JVMDefalteCmpProvider() }
    private val gzip: JVMGzipCmpProvider by lazy { JVMGzipCmpProvider() }
    private val lz4: JvmLz4CmpProvider by lazy { JvmLz4CmpProvider() }
    private val ztd: JvmZTDCmpProvider by lazy { JvmZTDCmpProvider() }

    override fun isAlgoAvailable(algo: CompressionAlgo): Boolean {
        // JVM supports all
        return true
    }

    override suspend fun compress(bytes: ByteArray, algo: CompressionAlgo): ByteArray {
        val output = when (algo) {
            CompressionAlgo.COMPRESS_NONE -> bytes
            CompressionAlgo.COMPRESS_LZ4 -> lz4.compress(bytes)
            CompressionAlgo.COMPRESS_ZSTD -> ztd.compress(bytes)
            CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> defalte.compress(bytes)
            CompressionAlgo.COMPRESS_GZIP -> gzip.compress(bytes)
        }
        return output
    }

    override suspend fun unCompress(bytes: ByteArray, algo: CompressionAlgo): ByteArray {
        val output = when (algo) {
            CompressionAlgo.COMPRESS_NONE -> bytes
            CompressionAlgo.COMPRESS_LZ4 -> lz4.unCompress(bytes)
            CompressionAlgo.COMPRESS_ZSTD -> ztd.unCompress(bytes)
            CompressionAlgo.COMPRESS_MINIZ_DEFLATE -> defalte.unCompress(bytes)
            CompressionAlgo.COMPRESS_GZIP -> gzip.unCompress(bytes)
        }
        return output
    }

    override fun close() = Unit
}
