package com.sam.bluepad.compression.algos

import com.sam.bluepad.compression.DataCompressorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class JVMDefalteCmpProvider : DataCompressorProvider {

    override suspend fun compress(bytes: ByteArray): ByteArray {
        val deflater = Deflater().apply {
            setInput(bytes)
            finish()
        }
        return withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            deflater.end()
            outputStream.toByteArray()
        }
    }

    override suspend fun unCompress(bytes: ByteArray): ByteArray {
        val inflater = Inflater().apply { setInput(bytes) }

        return withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            outputStream.toByteArray()
        }
    }
}
