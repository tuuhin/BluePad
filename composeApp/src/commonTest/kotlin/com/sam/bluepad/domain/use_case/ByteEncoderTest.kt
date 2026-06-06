package com.sam.bluepad.domain.use_case

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.Test

class ByteEncoderTest : KoinTest {

    private val _encoder by inject<BytesEncoder>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        modules(module { singleOf(::BytesEncoder) })
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @Test
    fun check_proper_encoding_and_decoding() = runTest {
        val someText = "Some text"

        val bytes = _encoder.decodeBytes(someText)
        val result = _encoder.encodeBytes(bytes)

        assertThat(result).isEqualTo(someText)
    }
}
