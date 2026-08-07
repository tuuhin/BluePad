package com.sam.bluepad.compression

import com.sam.bluepad.compression.native.macos.ZSTD_CONTENTSIZE_ERROR
import com.sam.bluepad.compression.native.macos.ZSTD_CONTENTSIZE_UNKNOWN
import com.sam.bluepad.compression.native.macos.ZSTD_compress
import com.sam.bluepad.compression.native.macos.ZSTD_compressBound
import com.sam.bluepad.compression.native.macos.ZSTD_decompress
import com.sam.bluepad.compression.native.macos.ZSTD_getErrorName
import com.sam.bluepad.compression.native.macos.ZSTD_getFrameContentSize
import com.sam.bluepad.compression.native.macos.ZSTD_isError
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDataCompressionAlgorithmZlib
import platform.Foundation.compressedDataUsingAlgorithm
import platform.Foundation.decompressedDataUsingAlgorithm
import platform.darwin.COMPRESSION_LZ4
import platform.darwin.COMPRESSION_ZLIB
import platform.darwin.compression_algorithm
import platform.darwin.compression_decode_buffer
import platform.darwin.compression_encode_buffer

@OptIn(ExperimentalForeignApi::class)
actual class PlatformDataCompressor {

    actual val isLz4Available: Boolean = true
    actual val isZStdAvailable: Boolean = true
    actual val isGzipAvailable: Boolean = true
    actual val isDeflateAvailable: Boolean = true

    actual fun compressWithLz4(bytes: ByteArray): ByteArray {
        return compressBytes(bytes, COMPRESSION_LZ4)
    }

    actual fun unCompressWithLz4(bytes: ByteArray): ByteArray {
        return decompressBytes(bytes, COMPRESSION_LZ4)
    }

    actual fun compressWIthZStd(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return ByteArray(0)

        val srcSize = bytes.size.toULong()
        val maxDstSize = ZSTD_compressBound(srcSize)
        val dstBuffer = ByteArray(maxDstSize.toInt())

        bytes.usePinned { pinnedSrc ->
            dstBuffer.usePinned { pinnedDst ->
                val compressedSize = ZSTD_compress(
                    pinnedDst.addressOf(0),
                    maxDstSize,
                    pinnedSrc.addressOf(0),
                    srcSize,
                    3,
                )

                if (ZSTD_isError(compressedSize) != 0u) {
                    val errorName = ZSTD_getErrorName(compressedSize)?.toKString()
                    throw IllegalStateException("ZSTD compression failed: $errorName")
                }
                return dstBuffer.copyOf(compressedSize.toInt())
            }
        }
    }

    actual fun unCompressWithZtd(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return ByteArray(0)

        return bytes.usePinned { pinnedSrc ->
            val decompressedBound = ZSTD_getFrameContentSize(pinnedSrc.addressOf(0), bytes.size.toULong())

            val dstCapacity =
                if (decompressedBound != ZSTD_CONTENTSIZE_UNKNOWN && decompressedBound != ZSTD_CONTENTSIZE_ERROR)
                    decompressedBound.toInt()
                else bytes.size * 4


            val dstBuffer = ByteArray(dstCapacity)

            dstBuffer.usePinned { pinnedDst ->
                val actualSize = ZSTD_decompress(
                    pinnedDst.addressOf(0),
                    dstBuffer.size.toULong(),
                    pinnedSrc.addressOf(0),
                    bytes.size.toULong(),
                )

                if (ZSTD_isError(actualSize) == 0u) {
                    return dstBuffer.copyOf(actualSize.toInt())
                }
            }
            dstBuffer
        }
    }

    actual fun compressWithGzip(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return ByteArray(0)

        val data = bytes.toNSData()
        // Compression with 31 windowBits produces GZIP format in POSIX zlib
        val compressedData = data.compressedDataUsingAlgorithm(
            NSDataCompressionAlgorithmZlib,
            error = null,
        ) ?: throw IllegalStateException("Gzip compression failed")

        return compressedData.toByteArray()
    }

    actual fun unCompressWithGzip(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return ByteArray(0)

        val data = bytes.toNSData()
        val decompressedData = data.decompressedDataUsingAlgorithm(
            NSDataCompressionAlgorithmZlib,
            error = null,
        ) ?: throw IllegalStateException("Gzip decompression failed")

        return decompressedData.toByteArray()
    }

    actual fun compressWithDeflate(bytes: ByteArray): ByteArray {
        return compressBytes(bytes, COMPRESSION_ZLIB)
    }

    actual fun uncompressWithDeflate(bytes: ByteArray): ByteArray {
        return decompressBytes(bytes, COMPRESSION_ZLIB)
    }


    @OptIn(ExperimentalForeignApi::class)
    private fun compressBytes(bytes: ByteArray, algorithm: compression_algorithm): ByteArray = memScoped {
        if (bytes.isEmpty()) return@memScoped ByteArray(0)

        val dstCapacity = bytes.size * 2 + 64
        val dstBuffer = allocArray<UByteVar>(dstCapacity)

        val srcSize = bytes.size.toULong()

        val compressedSize = bytes.usePinned { srcBuffer ->
            compression_encode_buffer(
                dstBuffer.pointed.ptr,
                dstCapacity.toULong(),
                srcBuffer.addressOf(0).reinterpret(),
                srcSize,
                null,
                algorithm,
            )
        }
        if (compressedSize == 0UL) {
            throw IllegalStateException("Compression failed for algorithm: $algorithm")
        }
        return dstBuffer.readBytes(compressedSize.toInt())
    }

    private fun decompressBytes(
        bytes: ByteArray,
        algorithm: compression_algorithm
    ): ByteArray {
        if (bytes.isEmpty()) return ByteArray(0)

        var dstCapacity = bytes.size * 4
        var dstBuffer = ByteArray(dstCapacity)

        bytes.usePinned { pinnedSrc ->
            while (true) {
                dstBuffer.usePinned { pinnedDst ->
                    val decompressedSize = compression_decode_buffer(
                        pinnedDst.addressOf(0).reinterpret(),
                        dstCapacity.toULong(),
                        pinnedSrc.addressOf(0).reinterpret(),
                        bytes.size.toULong(),
                        null,
                        algorithm,
                    )

                    if (decompressedSize > 0UL)
                        return dstBuffer.copyOf(decompressedSize.toInt())
                    // Expand buffer if decompressed size exceeds allocated space
                    dstCapacity *= 2
                    dstBuffer = ByteArray(dstCapacity)
                }
            }
        }
        return dstBuffer
    }
}
