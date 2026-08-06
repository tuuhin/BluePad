package com.sam.bluepad.compression.algos

import com.sam.bluepad.compression.DataCompressorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.jpountz.lz4.LZ4Factory

class JvmLz4CmpProvider : DataCompressorProvider {

    private val compressor by lazy { LZ4Factory.fastestInstance().fastCompressor() }
    private val decompressor by lazy { LZ4Factory.fastestInstance().fastDecompressor() }

    override suspend fun compress(bytes: ByteArray): ByteArray {
        val maxCompressedLength = compressor.maxCompressedLength(bytes.size)

        return withContext(Dispatchers.Default) {
            val compressed = ByteArray(maxCompressedLength + 4)
            // Store original size in first 4 bytes for raw buffer decompression
            compressed[0] = (bytes.size shr 24).toByte()
            compressed[1] = (bytes.size shr 16).toByte()
            compressed[2] = (bytes.size shr 8).toByte()
            compressed[3] = bytes.size.toByte()

            val compressedLength = compressor.compress(bytes, 0, bytes.size, compressed, 4, maxCompressedLength)
            compressed.copyOf(compressedLength + 4)
        }
    }

    override suspend fun unCompress(bytes: ByteArray): ByteArray {
        val originalSize = ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)

        return withContext(Dispatchers.Default) {
            val restored = ByteArray(originalSize)
            decompressor.decompress(bytes, 4, restored, 0, originalSize)
            restored
        }
    }
}
