package com.sam.bluepad.compression

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEqualTo
import com.sam.bluepad.compression.model.CompressionAlgo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class NativeCompressionTest {
    private val _controller = NativeDataCompressor()

    @Test
    fun `test basic compression and un compression with lz4`() = runTest {
        val algo = CompressionAlgo.COMPRESS_LZ4
        val someBytes = "ABCDEFGHIJKLMNOPQRSTWXYZ".repeat(1_000).encodeToByteArray()
        val result = _controller.compress(someBytes, algo)

        assertThat(someBytes).isNotEqualTo(result)
        assertThat(someBytes.size).isGreaterThanOrEqualTo(result.size)

        val unCompress = _controller.unCompress(result, algo)
        assertThat(someBytes).isEqualTo(unCompress)
    }
}
