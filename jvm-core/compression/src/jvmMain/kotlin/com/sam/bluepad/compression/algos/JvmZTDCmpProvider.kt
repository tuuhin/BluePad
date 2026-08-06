package com.sam.bluepad.compression.algos

import com.github.luben.zstd.Zstd
import com.sam.bluepad.compression.DataCompressorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmZTDCmpProvider : DataCompressorProvider {

    override suspend fun compress(bytes: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) { Zstd.compress(bytes) }
    }

    override suspend fun unCompress(bytes: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val originalSize = Zstd.getFrameContentSize(bytes)
            if (originalSize <= 0) {
                val size = (bytes.size * 10).coerceAtLeast(1024)
                Zstd.decompress(bytes, size)
            }
            Zstd.decompress(bytes, originalSize.toInt())
        }
    }
}
