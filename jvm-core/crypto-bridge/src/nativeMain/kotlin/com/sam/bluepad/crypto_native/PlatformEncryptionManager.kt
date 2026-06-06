package com.sam.bluepad.crypto_native

expect class PlatformEncryptionManager : EncryptionManager {

    override fun decryptData(bytes: ByteArray): ByteArray
    override fun encryptData(bytes: ByteArray): ByteArray
}
