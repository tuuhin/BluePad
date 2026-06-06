package com.sam.bluepad.data.utils

import kotlinx.coroutines.CoroutineDispatcher

expect class PlatformDispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
