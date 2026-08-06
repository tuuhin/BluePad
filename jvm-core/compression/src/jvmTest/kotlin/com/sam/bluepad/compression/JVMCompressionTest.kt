package com.sam.bluepad.compression

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEqualTo
import com.sam.bluepad.compression.model.CompressionAlgo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class JVMCompressionTest {

    private val _controller = JVMDataCompressor()

    @Test
    fun `test basic compression and un compression with lz4`() = runTest {
        val someBytes = "ABCDEFGHIJKLMNOPQRSTWXYZ".repeat(1_000).encodeToByteArray()
        val result = _controller.compress(someBytes, algo = CompressionAlgo.COMPRESS_LZ4)

        assertThat(someBytes).isNotEqualTo(result)
        assertThat(someBytes.size).isGreaterThanOrEqualTo(result.size)

        val unCompress = _controller.unCompress(result, CompressionAlgo.COMPRESS_LZ4)
        assertThat(someBytes).isEqualTo(unCompress)
    }
}
