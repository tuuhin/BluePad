package com.sam.bluepad.compression

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.sam.bluepad.compression.model.CompressionAlgo
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class JVMCompressionTest {

    lateinit var compressor: DataCompressor

    @BeforeTest
    fun setup() {
        compressor = JVMDataCompressor()
    }

    @AfterTest
    fun tearDown() {
        compressor.close()
    }

    @Test
    fun `test basic compression and un compression with lz4`() = runTest {
        val algo = CompressionAlgo.COMPRESS_LZ4
        val someBytes = "The quick brown fox jumps over the lazy dogs.".encodeToByteArray()
        val result = compressor.compress(someBytes, algo)

        assertThat(someBytes).isNotEmpty()

        val unCompress = compressor.unCompress(result, algo)
        assertThat(unCompress).isNotEmpty()

        assertThat(someBytes).isEqualTo(unCompress)
    }

    @Test
    fun `test basic compression and un compression with zstd`() = runTest {
        val algo = CompressionAlgo.COMPRESS_ZSTD
        val someBytes = "The quick brown fox jumps over the lazy dogs.".encodeToByteArray()
        val result = compressor.compress(someBytes, algo)

        assertThat(someBytes).isNotEmpty()

        val unCompress = compressor.unCompress(result, algo)
        assertThat(unCompress).isNotEmpty()

        assertThat(someBytes).isEqualTo(unCompress)
    }

    @Test
    fun `test basic compression and un compression with deflate`() = runTest {
        val algo = CompressionAlgo.COMPRESS_MINIZ_DEFLATE
        val someBytes = "The quick brown fox jumps over the lazy dogs.".encodeToByteArray()
        val result = compressor.compress(someBytes, algo)

        assertThat(someBytes).isNotEmpty()

        val unCompress = compressor.unCompress(result, algo)
        assertThat(unCompress).isNotEmpty()

        assertThat(someBytes).isEqualTo(unCompress)
    }

    @Test
    fun `test basic compression and un compression with gzip`() = runTest {
        val algo = CompressionAlgo.COMPRESS_GZIP
        val someBytes = "The quick brown fox jumps over the lazy dogs.".encodeToByteArray()
        val result = compressor.compress(someBytes, algo)

        assertThat(someBytes).isNotEmpty()

        val unCompress = compressor.unCompress(result, algo)
        assertThat(unCompress).isNotEmpty()

        assertThat(someBytes).isEqualTo(unCompress)
    }
}
