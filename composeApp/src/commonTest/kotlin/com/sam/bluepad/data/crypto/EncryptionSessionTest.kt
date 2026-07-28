package com.sam.bluepad.data.crypto

import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.testModule
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.crypto.KeyEncryptionManager
import com.sam.bluepad.domain.crypto.exception.MissingKeyFileException
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptionSessionTest : KoinTest {

    private val sessionId = Uuid.random()
    private val sessionManager by inject<EncryptionSessionManager>()
    private val keyEncryptionManager by inject<KeyEncryptionManager>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        modules(createPlatformModule() + commonAppModule)
        loadKoinModules(testModule)
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @AfterTest
    fun tearDown() = runTest {
        sessionManager.deleteSessionData(sessionId)
        advanceUntilIdle()
        keyEncryptionManager.clearKey()
    }

    @Test
    fun check_if_we_are_able_to_encrypt_the_object_and_then_decrypt_it_with_the_keys() = runTest {
        val randomBytes = "Hey this is great right??".encodeToByteArray()

        sessionManager.encryptDataAndSave(sessionId = sessionId, randomBytes)
        advanceUntilIdle()

        val bytes = sessionManager.decryptAndReadData(sessionId)

        assertContentEquals(randomBytes, bytes)
    }

    @Test
    fun check_if_we_are_able_to_encrypt_the_object_and_delete_the_key_and_try_to_decrypt_it() = runTest {
        val randomBytes = "Hey this is great right??".encodeToByteArray()

        sessionManager.encryptDataAndSave(sessionId = sessionId, randomBytes)
        advanceUntilIdle()

        sessionManager.deleteSessionData(sessionId)
        advanceUntilIdle()

        assertFailsWith<MissingKeyFileException> {
            sessionManager.decryptAndReadData(sessionId)
        }
    }

    @Test
    fun decrypting_non_existent_session_throws_exception() = runTest {
        val nonExistentSessionId = Uuid.random()

        assertFailsWith<MissingKeyFileException> {
            sessionManager.decryptAndReadData(nonExistentSessionId)
        }
    }

    @Test
    fun clearing_master_key_prevents_decryption_of_existing_session() = runTest {
        val payload = "Secret message".encodeToByteArray()

        sessionManager.encryptDataAndSave(sessionId, payload)
        advanceUntilIdle()
        keyEncryptionManager.clearKey()

        assertFailsWith<MissingKeyFileException> {
            sessionManager.decryptAndReadData(sessionId)
        }
    }

    @Test
    fun sessions_are_isolated_and_cannot_read_each_others_data() = runTest {
        val sessionA = Uuid.random()
        val sessionB = Uuid.random()

        val dataA = "Data for Session A".encodeToByteArray()
        val dataB = "Data for Session B".encodeToByteArray()

        try {
            sessionManager.encryptDataAndSave(sessionA, dataA)
            sessionManager.encryptDataAndSave(sessionB, dataB)
            advanceUntilIdle()

            val decryptedA = sessionManager.decryptAndReadData(sessionA)
            val decryptedB = sessionManager.decryptAndReadData(sessionB)

            assertContentEquals(dataA, decryptedA)
            assertContentEquals(dataB, decryptedB)
        } finally {
            // Clean up secondary session
            sessionManager.deleteSessionData(sessionA)
            sessionManager.deleteSessionData(sessionB)
        }
    }

    @Test
    fun overwriting_session_data_updates_payload_correctly() = runTest {
        val initialData = "Initial confidential text".encodeToByteArray()
        val updatedData = "Overwritten new confidential text".encodeToByteArray()

        sessionManager.encryptDataAndSave(sessionId, initialData)
        advanceUntilIdle()

        // Overwrite existing session payload
        sessionManager.encryptDataAndSave(sessionId, updatedData)
        advanceUntilIdle()

        val result = sessionManager.decryptAndReadData(sessionId)

        assertContentEquals(updatedData, result)
    }

    @Test
    fun encrypt_and_decrypt_empty_byte_array() = runTest {
        val emptyBytes = byteArrayOf()

        sessionManager.encryptDataAndSave(sessionId, emptyBytes)
        advanceUntilIdle()

        val decrypted = sessionManager.decryptAndReadData(sessionId)

        assertContentEquals(emptyBytes, decrypted)
    }
}
