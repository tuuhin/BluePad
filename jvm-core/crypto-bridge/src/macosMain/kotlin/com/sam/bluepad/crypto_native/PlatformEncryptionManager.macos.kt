package com.sam.bluepad.crypto_native

import kotlinx.cinterop.IntVar
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.kSecClass
import platform.Security.kSecReturnRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFNumberIntType
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECIESEncryptionStandardX963SHA256AESGCM
import platform.Security.kSecPrivateKeyAttrs

actual class PlatformEncryptionManager : EncryptionManager {

    private val keyTag = "com.sam.bluepad.crypto_encryption_manager"

    actual override fun decryptData(bytes: ByteArray): ByteArray = memScoped {

        val privateKey = getSecureKey(false) ?: return byteArrayOf()
        val errorRef = alloc<CFErrorRefVar>()

        val cfDataIn = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }

        val cfDataOut = SecKeyCreateDecryptedData(
            privateKey,
            kSecKeyAlgorithmECIESEncryptionStandardX963SHA256AESGCM,
            cfDataIn,
            errorRef.ptr,
        )

        try {
            if (cfDataOut == null) return byteArrayOf()

            val length = CFDataGetLength(cfDataOut).toInt()
            val bytePtr = CFDataGetBytePtr(cfDataOut)
            bytePtr!!.readBytes(length)
        } finally {
            if (cfDataOut != null) CFRelease(cfDataOut)
            if (cfDataIn != null) CFRelease(cfDataIn)
            CFRelease(privateKey)
        }

    }

    actual override fun encryptData(bytes: ByteArray): ByteArray = memScoped {
        val publicKey = getSecureKey(true) ?: return byteArrayOf()
        val errorRef = alloc<CFErrorRefVar>()

        val cfDataIn = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }

        val cfDataOut = SecKeyCreateEncryptedData(
            publicKey,
            kSecKeyAlgorithmECIESEncryptionStandardX963SHA256AESGCM,
            cfDataIn,
            errorRef.ptr,
        )

        try {
            if (cfDataOut == null) return byteArrayOf()

            val length = CFDataGetLength(cfDataOut).toInt()
            val bytePtr = CFDataGetBytePtr(cfDataOut)
            bytePtr!!.readBytes(length)
        } finally {
            if (cfDataOut != null) CFRelease(cfDataOut)
            if (cfDataIn != null) CFRelease(cfDataIn)
            CFRelease(publicKey)
        }
    }

    actual override fun cleanUpKeys() = memScoped {
        val tagData = keyTag.encodeToByteArray()
        val cfTag = tagData.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), tagData.size.toLong())
        }

        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassKey)
        CFDictionarySetValue(query, kSecAttrApplicationTag, cfTag)

        try {
            val status = SecItemDelete(query)
            if (status != errSecSuccess && status != errSecItemNotFound)
                throw RuntimeException("Failed to clean the crypto key or key alias dont exists status:$status")

        } finally {
            if (query != null) CFRelease(query)
            if (cfTag != null) CFRelease(cfTag)
        }
    }

    private fun getSecureKey(isPublic: Boolean = false): SecKeyRef? = memScoped {

        val tagData = keyTag.encodeToByteArray()
        var cfTag: platform.CoreFoundation.CFDataRef? = null
        var query: platform.CoreFoundation.CFMutableDictionaryRef? = null
        var privateKeyAttrs: platform.CoreFoundation.CFMutableDictionaryRef? = null
        var cfKeySize: platform.CoreFoundation.CFNumberRef? = null
        var publicKeyAttrs: platform.CoreFoundation.CFMutableDictionaryRef? = null

        try {
            cfTag = tagData.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0).reinterpret(), tagData.size.toLong())
            }

            query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassKey)
            CFDictionarySetValue(query, kSecAttrApplicationTag, cfTag)
            CFDictionarySetValue(query, kSecReturnRef, kCFBooleanTrue)

            val resultRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultRef.ptr)

            if (status == errSecSuccess) {
                val privateKey: SecKeyRef = resultRef.value?.reinterpret() ?: return null

                return if (isPublic) {
                    SecKeyCopyPublicKey(privateKey).also { CFRelease(privateKey) }
                } else {
                    privateKey
                }
            }

            privateKeyAttrs = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(privateKeyAttrs, kSecAttrIsPermanent, kCFBooleanTrue)
            CFDictionarySetValue(privateKeyAttrs, kSecAttrApplicationTag, cfTag)

            val keySizeBits = 256
            cfKeySize = CFNumberCreate(null, kCFNumberIntType, alloc<IntVar>().apply { value = keySizeBits }.ptr)

            publicKeyAttrs = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(publicKeyAttrs, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
            CFDictionarySetValue(publicKeyAttrs, kSecAttrKeySizeInBits, cfKeySize)
            CFDictionarySetValue(publicKeyAttrs, kSecPrivateKeyAttrs, privateKeyAttrs)

            val errorRef = alloc<CFErrorRefVar>()
            val newPrivateKey = SecKeyCreateRandomKey(publicKeyAttrs, errorRef.ptr) ?: return null

            return if (isPublic) {
                SecKeyCopyPublicKey(newPrivateKey).also { CFRelease(newPrivateKey) }
            } else {
                newPrivateKey
            }
        } finally {
            query?.let { CFRelease(it) }
            publicKeyAttrs?.let { CFRelease(it) }
            privateKeyAttrs?.let { CFRelease(it) }
            cfKeySize?.let { CFRelease(it) }
            cfTag?.let { CFRelease(it) }
        }
    }
}
