package com.sam.bluepad.data.crypto

import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.createPlatformTestModule
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.crypto.KeyEncryptionManager
import com.sam.bluepad.domain.crypto.exception.MissingKeyFileException
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
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
        // include the platform module
        modules(createPlatformTestModule() + createPlatformModule() + commonAppModule)

        // load the test module here
        loadKoinModules(module(createdAtStart = true) { singleOf(::TestCryptoFileProvider) bind CryptoFilePathProvider::class })
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @AfterTest
    fun tearDown() = runTest {
        sessionManager.deleteSessionData(sessionId)
        advanceUntilIdle()
        // delete the associated key
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
}
