package com.sam.bluepad.common.shared

import org.koin.core.annotation.Singleton
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

@Singleton(binds = [IHashGenerator::class])
class SHA2HashGenerator : IHashGenerator {


    private val hasher by lazy { SHA256() }

    override fun generateHash(hash: String): String {
        val bytes = hashString.encodeToByteArray()
        val resultBytes = hasher.digest(bytes)
        return Base64.encode(resultBytes)
    }
}
