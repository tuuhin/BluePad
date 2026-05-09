package com.sam.bluepad.data.crypto.encryption

import com.sam.bluepad.domain.crypto.EncryptionManager
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES

class AESCBCEncryptionManager private constructor(
    private val cryptoProvider: CryptographyProvider
) : EncryptionManager {

    constructor() : this(CryptographyProvider.Default)

    private val algo by lazy { cryptoProvider.get(AES.CBC) }

    override suspend fun encrypt(key: ByteArray, data: ByteArray): ByteArray {
        val aesKey = algo.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = aesKey.cipher()
        return cipher.encrypt(data)
    }

    override suspend fun decrypt(key: ByteArray, data: ByteArray): ByteArray {
        val aesKey = algo.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = aesKey.cipher()
        return cipher.decrypt(data)
    }
}
