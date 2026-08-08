package com.sam.bluepad.compression

import com.sam.bluepad.compression.native.mingw.compression_deflate_algo
import com.sam.bluepad.compression.native.mingw.compression_gzip
import com.sam.bluepad.compression.native.mingw.compression_lz4
import com.sam.bluepad.compression.native.mingw.compression_result
import com.sam.bluepad.compression.native.mingw.compression_ztd
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.memcpy
import platform.posix.size_tVar
import platform.posix.uint8_tVar

actual class PlatformDataCompressor {

    actual val isLz4Available: Boolean = true
    actual val isZStdAvailable: Boolean = true
    actual val isGzipAvailable: Boolean = true
    actual val isDeflateAvailable: Boolean = true

    actual fun compressWithLz4(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 2 + 256,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_lz4(COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun unCompressWithLz4(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 4 + 1024,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_lz4(UN_COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun compressWIthZStd(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 2 + 256,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_ztd(COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun unCompressWithZtd(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 4 + 1024,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_ztd(UN_COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun compressWithGzip(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 2 + 256,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_gzip(COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)

    }

    actual fun unCompressWithGzip(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 4 + 1024,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_gzip(UN_COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun compressWithDeflate(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 2 + 256,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_deflate_algo(COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    actual fun uncompressWithDeflate(bytes: ByteArray): ByteArray = executeCompression(
        bytes,
        capacity = bytes.size * 4 + 1024,
    ) { inPtr, inSize, outPtr, outCap, outSizePtr ->
        compression_deflate_algo(UN_COMPRESS_MODE, inPtr, inSize, outPtr, outCap, outSizePtr)
    }

    private inline fun executeCompression(
        input: ByteArray,
        capacity: Int,
        cFunction: (input: CPointer<uint8_tVar>?, inputSize: ULong, output: CPointer<uint8_tVar>?, outputCapacity: ULong, outputSize: CPointer<size_tVar>) -> compression_result
    ): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        return memScoped {
            val outputCapacity = capacity.coerceAtLeast(1024)
            val outputPtr = allocArray<uint8_tVar>(outputCapacity)
            val outputSize = alloc<size_tVar>()

            // Pin input ByteArray directly to obtain native pointer without copying
            val result = input.usePinned { pinnedInput ->
                val inputPtr = pinnedInput.addressOf(0).reinterpret<uint8_tVar>()
                cFunction(
                    inputPtr,
                    input.size.convert(),
                    outputPtr,
                    outputCapacity.convert(),
                    outputSize.ptr,
                )
            }

            if (result < 0u) throw IllegalStateException("Compression/Decompression failed with code: $result")

            // Copy output buffer to Kotlin ByteArray
            val actualSize = outputSize.value.toInt()
            if (actualSize <= 0) throw IllegalStateException("Result contain no result")
            val resultArray = ByteArray(actualSize)
            resultArray.usePinned { pinnedResult ->
                memcpy(pinnedResult.addressOf(0), outputPtr, outputSize.value)
            }
            resultArray
        }
    }

    companion object {
        private val COMPRESS_MODE = 0u
        private val UN_COMPRESS_MODE = 1u
    }
}
