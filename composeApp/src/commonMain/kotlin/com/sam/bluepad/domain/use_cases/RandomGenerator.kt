package com.sam.bluepad.domain.use_cases

interface RandomGenerator {

	fun generateRandomBytes(size: Int = 12): ByteArray
}