package com.sam.bluepad.crypto.data.secrets

import com.sam.bluepad.crypto.domain.IEncryptionManager
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import org.koin.core.annotation.Factory

@Factory(binds = [IEncryptionManager::class])
class AESCBCEncryptionManager(
    private val cryptoProvider: CryptographyProvider = CryptographyProvider.Default
) : IEncryptionManager {


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
