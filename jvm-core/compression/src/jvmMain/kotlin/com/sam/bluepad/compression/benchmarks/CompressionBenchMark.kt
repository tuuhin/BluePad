package com.sam.bluepad.compression.benchmarks

import com.sam.bluepad.compression.CompressionBackend
import com.sam.bluepad.compression.DataCompressor
import com.sam.bluepad.compression.JVMDataCompressor
import com.sam.bluepad.compression.NativeDataCompressor
import com.sam.bluepad.compression.model.CompressionAlgo
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Level
import kotlin.random.Random

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.SECONDS)
class CompressionBenchMark {


    @Param("JVM", "NATIVE")
    lateinit var engine: CompressionBackend

    @Param("COMPRESS_LZ4", "COMPRESS_ZSTD", "COMPRESS_MINIZ_DEFLATE", "COMPRESS_GZIP")
    lateinit var compressionAlgo: CompressionAlgo

    private lateinit var compressor: DataCompressor
    private lateinit var randomBytes: ByteArray

    @Setup(Level.Trial)
    fun setup() {
        compressor = when (engine) {
            CompressionBackend.JVM -> JVMDataCompressor()
            CompressionBackend.NATIVE -> NativeDataCompressor()
        }
        randomBytes = ByteArray(1024)
        Random.nextBytes(randomBytes)
    }

    @TearDown(Level.Trial)
    fun cleanUp() {
        compressor.close()
        randomBytes.fill(0)
    }

    @Benchmark
    fun compressAndUncompress() = runBlocking {
        val result = compressor.compress(randomBytes, compressionAlgo)
        compressor.unCompress(result, compressionAlgo)
    }

}
