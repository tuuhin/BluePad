package com.sam.bluepad.crypto_native

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
interface EncryptionManager {

    @CName("encrypt_data")
    fun encryptData(bytes: ByteArray): ByteArray

    @CName("decrypt_data")
    fun decryptData(bytes: ByteArray): ByteArray
}
