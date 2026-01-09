package com.sam.bluepad.domain.utils

sealed interface Resource<out S, out E : Exception> {

	data class Success<S, E : Exception>(
		val data: S,
		val message: String? = null
	) : Resource<S, E>

	data class Error<S, E : Exception>(
		val error: Exception,
		val message: String? = null,
		val data: S? = null
	) : Resource<S, E>

	data object Loading : Resource<Nothing, Nothing>
}