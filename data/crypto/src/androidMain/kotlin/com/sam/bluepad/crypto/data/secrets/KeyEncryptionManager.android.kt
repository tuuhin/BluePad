package com.sam.bluepad.crypto.data.secrets

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import co.touchlab.kermit.Logger
import com.sam.bluepad.crypto.domain.IPlatformKeyEncryptionManager
import com.sam.bluepad.model.crypto.EncryptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val TAG = "SECURE_KEY_MANAGER"

@Factory(binds = [IPlatformKeyEncryptionManager::class])
internal actual class KeyEncryptionManager : IPlatformKeyEncryptionManager {


    private val keyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    private val keyGenerator by lazy {
        val generator = KeyGenerator.getInstance(ENCRYPTION_ALGO, PROVIDER)

        val paramsSpec = KeyGenParameterSpec
            .Builder(SECURITY_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(paramsSpec)
        generator
    }

    val secretKey: SecretKey
        get() = keyStore.getKey(SECURITY_KEY_ALIAS, null) as? SecretKey ?: run {
            Logger.d(tag = TAG) { "CREATING A NEW KEY WE DON'T HAVE ANY KEY" }
            keyGenerator.generateKey()
        }


    actual override suspend fun encryptKey(key: ByteArray): EncryptionResult {
        return withContext(Dispatchers.Default) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.ENCRYPT_MODE, secretKey) }
            val ciphertext: ByteArray = cipher.doFinal(key)
            EncryptionResult(
                encrypted = ciphertext,
                encryptedSize = ciphertext.size,
                iv = cipher.iv,
                ivSize = cipher.iv.size,
            )
        }
    }

    actual override suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv)) }
            val ciphertext: ByteArray = cipher.doFinal(key)
            ciphertext
        }
    }

    actual override fun clearKey() {
        keyStore.deleteEntry(SECURITY_KEY_ALIAS)
    }

    companion object {


        // encryption properties
        private const val ENCRYPTION_ALGO = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

        private const val TRANSFORMATION = "$ENCRYPTION_ALGO/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

        private const val PROVIDER = "AndroidKeyStore"
        private const val SECURITY_KEY_ALIAS = "bluepad_super_secret_key_alias"
    }
}
