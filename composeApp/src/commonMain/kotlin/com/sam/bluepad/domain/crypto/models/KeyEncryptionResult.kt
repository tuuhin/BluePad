package com.sam.bluepad.domain.crypto.models

data class KeyEncryptionResult(
    val encrypted: ByteArray,
    val encryptedSize: Int,
    val iv: ByteArray = byteArrayOf(),
    val ivSize: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyEncryptionResult

        if (encryptedSize != other.encryptedSize) return false
        if (ivSize != other.ivSize) return false
        if (!encrypted.contentEquals(other.encrypted)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedSize
        result = 31 * result + ivSize
        result = 31 * result + encrypted.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }

}
