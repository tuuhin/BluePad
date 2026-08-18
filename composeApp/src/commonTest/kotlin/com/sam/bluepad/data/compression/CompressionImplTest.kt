package com.sam.bluepad.data.compression

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEmpty
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.createPlatformTestModule
import com.sam.bluepad.di.testModule
import com.sam.bluepad.domain.compression.ICompressionManager
import com.sam.bluepad.utils.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CompressionImplTest : KoinTest {

    private val compressor by inject<ICompressionManager>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        modules(createPlatformModule() + commonAppModule)
        loadKoinModules(createPlatformTestModule() + testModule)
    }

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    @Test
    fun compress_bytes_should_successfully_compress_valid_non_empty_byte_array() = runTest {
        val originalData = "Hello Bluepad Compression Test!".repeat(100).encodeToByteArray()

        val compressedBytes = compressor.compressBytes(originalData)
        assertThat(compressedBytes).isNotEmpty()
        assertThat(compressedBytes.size).isLessThan(originalData.size)
    }

    @Test
    fun compressBytes_should_throw_IllegalArgumentException_when_input_is_empty() = runTest {
        val emptyBytes = byteArrayOf()

        assertFailsWith<IllegalArgumentException> {
            compressor.compressBytes(emptyBytes)
        }
    }

    @Test
    fun inflateByte_should_correctly_restore_compressed_data() = runTest {
        val originalData = "I wonder when this project will have zero errors".repeat(500).encodeToByteArray()
        val compressedBytes = compressor.compressBytes(originalData)

        val decompressedBytes = compressor.inflateBytes(compressedBytes)
        assertThat(decompressedBytes).isEqualTo(originalData)
    }

    @Test
    fun inflateBytes_should_throw_IllegalArgumentException_when_input_is_empty() = runTest {
        val emptyBytes = byteArrayOf()

        assertFailsWith<IllegalArgumentException> {
            compressor.inflateBytes(emptyBytes)
        }
    }

    @Test
    fun round_trip_compression_and_inflation_preserves_exac_binary_payload() = runTest {
        val binaryPayload = ByteArray(2048) { it.toByte() }

        val compressed = compressor.compressBytes(binaryPayload)
        val decompressed = compressor.inflateBytes(compressed)
        assertThat(binaryPayload).isEqualTo(decompressed)
    }
}
