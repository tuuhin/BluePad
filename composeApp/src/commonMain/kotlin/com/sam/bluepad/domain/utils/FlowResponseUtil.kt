package com.sam.bluepad.domain.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

inline fun <T> handleDBOperation(crossinline operation: suspend FlowCollector<Resource<T, Exception>>.() -> Resource<T, Exception>): Flow<Resource<T, Exception>> {
	return flow {
		emit(Resource.Loading)
		try {
			emit(operation())
		} catch (e: Exception) {
			e.printStackTrace()
			emit(Resource.Error(error = e))
		}
	}
}