package com.sam.bluepad.data.compression

import co.touchlab.kermit.Logger
import com.github.luben.zstd.Zstd
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.compression.CompressionLevel
import com.sam.bluepad.domain.compression.ICompressionManager
import kotlinx.coroutines.withContext

private const val TAG = "COMPRESSION_MANAGER"

actual class CompressionManagerImpl(private val dispatchers: PlatformDispatcherProvider) : ICompressionManager {

    actual override suspend fun compressBytes(bytes: ByteArray, level: CompressionLevel): ByteArray {
        require(bytes.isNotEmpty()) { "Cannot compress empty bytes" }
        Logger.d(tag = TAG) { "USING COMPRESSION SETTINGS LEVEL :${level.level}" }
        return withContext(dispatchers.default) {
            Zstd.compress(bytes, level.level)
        }
    }

    actual override suspend fun inflateBytes(bytes: ByteArray): ByteArray {
        require(bytes.isNotEmpty()) { "Cannot inflate empty bytes" }
        return withContext(dispatchers.default) {
            val originalSize = Zstd.getFrameContentSize(bytes)
            if (originalSize <= 0) {
                val size = (bytes.size * 10).coerceAtLeast(1024)
                Zstd.decompress(bytes, size)
            }
            Zstd.decompress(bytes, originalSize.toInt())
        }
    }
}
