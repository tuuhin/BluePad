package com.sam.bluepad.compression

import com.sam.bluepad.compression.native.mingw.compression_deflate_algo
import com.sam.bluepad.compression.native.mingw.compression_gzip
import com.sam.bluepad.compression.native.mingw.compression_lz4
import com.sam.bluepad.compression.native.mingw.compression_ztd
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
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

    actual fun compressWithLz4(bytes: ByteArray) = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_lz4(
            mode = COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun unCompressWithLz4(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_lz4(
            mode = UN_COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun compressWIthZStd(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_ztd(
            mode = COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun unCompressWithZtd(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_ztd(
            mode = UN_COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun compressWithGzip(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_gzip(
            mode = COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun unCompressWithGzip(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_gzip(
            mode = UN_COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun compressWithDeflate(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_deflate_algo(
            mode = COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    actual fun uncompressWithDeflate(bytes: ByteArray): ByteArray = memScoped {
        val input = bytes.toUByteArray()

        val inputPtr = allocArray<uint8_tVar>(input.size)
        input.usePinned {
            memcpy(inputPtr, it.addressOf(0), input.size.convert())
        }

        // Allocate a larger buffer for compressed data.
        val outputCapacity = bytes.size * 2 + 256
        val outputPtr = allocArray<uint8_tVar>(outputCapacity)

        val outputSize = alloc<size_tVar>()

        val result = compression_deflate_algo(
            mode = UN_COMPRESS_MODE,
            input = inputPtr,
            input_size = input.size.convert(),
            output = outputPtr,
            output_capacity = outputCapacity.convert(),
            output_size = outputSize.ptr,
        )

        if (result != 0u) throw IllegalStateException("Compression failed: $result")
        val byteArray = ByteArray(outputSize.value.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), outputPtr, outputSize.value)
        }
        byteArray
    }

    companion object {
        private const val COMPRESS_MODE = 0u
        private const val UN_COMPRESS_MODE = 1u
    }
}
