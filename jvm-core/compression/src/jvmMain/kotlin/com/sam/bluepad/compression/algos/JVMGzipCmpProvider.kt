package com.sam.bluepad.compression.algos

import com.sam.bluepad.compression.DataCompressorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class JVMGzipCmpProvider : DataCompressorProvider {

    override suspend fun compress(bytes: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { it.write(bytes) }
            outputStream.toByteArray()
        }
    }

    override suspend fun unCompress(bytes: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
            val inputStream = ByteArrayInputStream(bytes)
            GZIPInputStream(inputStream).use { it.readBytes() }
        }
    }
}
