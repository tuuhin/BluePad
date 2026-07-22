package com.sam.bluepad.crypto_native

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.windows.CryptProtectData
import platform.windows.CryptUnprotectData
import platform.windows.DATA_BLOB
import platform.windows.LocalFree

actual class PlatformEncryptionManager : EncryptionManager {

    actual override fun encryptData(bytes: ByteArray): ByteArray = memScoped {
        val inputBlob = alloc<DATA_BLOB>()
        val outputBlob = alloc<DATA_BLOB>()

        bytes.usePinned { pinned ->
            inputBlob.cbData = bytes.size.toUInt()
            inputBlob.pbData = pinned.addressOf(0).reinterpret()

            val success = CryptProtectData(
                pDataIn = inputBlob.ptr,
                szDataDescr = null,
                pOptionalEntropy = null,
                pvReserved = null,
                pPromptStruct = null,
                dwFlags = 0.toUInt(),
                pDataOut = outputBlob.ptr,
            )

            if (success == 0) return@memScoped byteArrayOf()
            val result = outputBlob.pbData!!.readBytes(outputBlob.cbData.toInt())
            LocalFree(outputBlob.pbData)
            return@memScoped result
        }
    }

    actual override fun decryptData(bytes: ByteArray): ByteArray = memScoped {
        val inputBlob = alloc<DATA_BLOB>()
        val outputBlob = alloc<DATA_BLOB>()

        bytes.usePinned { pinned ->
            inputBlob.cbData = bytes.size.toUInt()
            inputBlob.pbData = pinned.addressOf(0).reinterpret()

            val success = CryptUnprotectData(
                pDataIn = inputBlob.ptr,
                ppszDataDescr = null,
                pOptionalEntropy = null,
                pvReserved = null,
                pPromptStruct = null,
                dwFlags = 0.toUInt(),
                pDataOut = outputBlob.ptr,
            )

            if (success == 0) return@memScoped byteArrayOf()
            val result = outputBlob.pbData!!.readBytes(outputBlob.cbData.toInt())
            LocalFree(outputBlob.pbData)
            return@memScoped result
        }
    }

    actual override fun cleanUpKeys() = Unit
}
