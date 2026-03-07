package com.sam.bluepad.domain.use_case

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sam.bluepad.domain.use_cases.BytesEncoder
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

    @Test
    fun `check proper encoding and decoding`() {
        val someText = "Some text"

        val bytes = _encoder.decodeBytes(someText)
        val result = _encoder.encodeBytes(bytes)

        assertThat(result).isEqualTo(someText)
    }
}