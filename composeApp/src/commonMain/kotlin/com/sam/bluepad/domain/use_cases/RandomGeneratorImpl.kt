package com.sam.bluepad.domain.use_cases

import org.kotlincrypto.random.CryptoRand

class RandomGeneratorImpl : RandomGenerator {

	override fun generateRandomBytes(size: Int): ByteArray {
		return CryptoRand.nextBytes(ByteArray(size))
	}
}