package com.sam.bluepad.domain.use_case

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.random.Random
import kotlin.test.Test

class ByteEncoderTest : KoinTest {

    private val _encoder by inject<BytesEncoder>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(module { single<BytesEncoder>() })
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @Test
    fun check_proper_encoding_and_decoding() = runTest {
        val someText = "Some crazy text".encodeToByteArray()

        val encodedString = _encoder.encodeBytes(someText)
        val result = _encoder.decodeBytes(encodedString)

        assertThat(result).isEqualTo(someText)
    }

    @Test
    fun encode_and_decode_empty_byte_array() = runTest {
        val emptyBytes = byteArrayOf()

        val encoded = _encoder.encodeBytes(emptyBytes)
        val decoded = _encoder.decodeBytes(encoded)

        assertThat(encoded).isEmpty()
        assertThat(decoded).isEmpty()
    }

    @Test
    fun encode_and_decode_special_characters_and_emojis() = runTest {
        val textWithSpecialChars = "Hello World! 🚀🔥 @#\\$%^&*()_+-=[]{}|;':\",./<>?"
            .encodeToByteArray()

        val encoded = _encoder.encodeBytes(textWithSpecialChars)
        val decoded = _encoder.decodeBytes(encoded)

        assertThat(decoded).isEqualTo(textWithSpecialChars)
    }

    @Test
    fun encode_and_decode_raw_binary_data() = runTest {
        val randomBinaryData = Random.nextBytes(256)

        val encoded = _encoder.encodeBytes(randomBinaryData)
        val decoded = _encoder.decodeBytes(encoded)

        assertThat(decoded).isEqualTo(randomBinaryData)
    }

    @Test
    fun encode_is_deterministic_for_same_input() = runTest {
        val sample = "Deterministic string test".encodeToByteArray()

        val encodedFirstTime = _encoder.encodeBytes(sample)
        val encodedSecondTime = _encoder.encodeBytes(sample)

        assertThat(encodedFirstTime).isEqualTo(encodedSecondTime)
    }

    @Test
    fun decode_invalid_base64_string_throws_exception() = runTest {
        val invalidBase64 = "This is definitely not valid Base64! %$$#"

        assertFailure {
            _encoder.decodeBytes(invalidBase64)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
