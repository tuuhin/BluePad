package com.sam.bluepad.domain.use_cases

fun interface RandomGenerator {

    suspend fun generateRandomBytes(size: Int): ByteArray
}
