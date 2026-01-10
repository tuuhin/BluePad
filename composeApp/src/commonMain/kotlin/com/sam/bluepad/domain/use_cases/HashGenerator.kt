package com.sam.bluepad.domain.use_cases

import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

class HashGenerator {

	val hasher by lazy { SHA256() }

	fun generateHash(hashString: String): String {
		val bytes = hashString.encodeToByteArray()
		val resultBytes = hasher.digest(bytes)
		return Base64.encode(resultBytes)
	}
}