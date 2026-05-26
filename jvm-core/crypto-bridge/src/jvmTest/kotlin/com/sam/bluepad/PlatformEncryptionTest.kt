package com.sam.bluepad

import com.sam.bluepad.platform.native.PlatformEncryptionManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformEncryptionTest {

    @AfterTest
    fun tearDown() = PlatformEncryptionManager().use { platform ->
        platform.cleanUpKeys()
    }

    @Test
    fun `check basic platform encryption working`() = PlatformEncryptionManager().use { platform ->

        val data = "Hello this is native platform encryption"
        val result = platform.encryptData(data.encodeToByteArray())
        assertTrue(result.isNotEmpty())

        val decryptedData = platform.decryptData(result)
        assertTrue(decryptedData.isNotEmpty())
        assertEquals(data, decryptedData.decodeToString())
    }

    @Test
    fun `check encryption with small data`() = PlatformEncryptionManager().use { platform ->
        val data = byteArrayOf(1)
        val encrypted = platform.encryptData(data)
        assertTrue(encrypted.isNotEmpty(), "Encrypted small data should not be empty")

        val decrypted = platform.decryptData(encrypted)
        assertEquals(1, decrypted.size)
        assertEquals(1, decrypted[0])
    }

    @Test
    fun `check encryption with large data`() = PlatformEncryptionManager().use { platform ->
        val size = 1024 * 64 // 64KB
        val data = ByteArray(size) { it.toByte() }

        val encrypted = platform.encryptData(data)
        assertTrue(encrypted.size > size)

        val decrypted = platform.decryptData(encrypted)
        assertEquals(size, decrypted.size)
        assertTrue(data.contentEquals(decrypted))
    }

    @Test
    fun `check multiple sequential operations`() = PlatformEncryptionManager().use { platform ->
        val messages = listOf("Message 1", "Another message", "Short", "A very long message to ensure variety")

        messages.forEach { msg ->
            val bytes = msg.encodeToByteArray()
            val encrypted = platform.encryptData(bytes)
            val decrypted = platform.decryptData(encrypted)
            assertEquals(msg, decrypted.decodeToString())
        }
    }

    @Test
    fun `check decryption of invalid data returns empty`() = PlatformEncryptionManager().use { platform ->
        val invalidData = byteArrayOf(1, 2, 3, 4, 5)
        val decrypted = platform.decryptData(invalidData)
        // DPAPI's CryptUnprotectData should fail on this and our implementation returns empty array on failure
        assertTrue(decrypted.isEmpty(), "Decryption of random bytes should fail and return empty array")
    }

    @Test
    fun `check encryption produces different results for same data`() = PlatformEncryptionManager().use { platform ->
        val data = "Static Data".encodeToByteArray()
        val enc1 = platform.encryptData(data)
        val enc2 = platform.encryptData(data)

        // DPAPI often includes some entropy or timestamp, so results might differ or be same,
        // but we definitely want to ensure they both decrypt to the same thing.
        assertTrue(!enc1.contentEquals(enc2), "Encrypted data should be different for same input")
        assertEquals(data.decodeToString(), platform.decryptData(enc1).decodeToString())
        assertEquals(data.decodeToString(), platform.decryptData(enc2).decodeToString())
    }
}

