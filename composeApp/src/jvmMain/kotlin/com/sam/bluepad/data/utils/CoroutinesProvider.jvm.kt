package com.sam.bluepad.data.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class PlatformDispatcherProvider {

    actual val main: CoroutineDispatcher
        get() = Dispatchers.Main

    actual val io: CoroutineDispatcher
        get() = Dispatchers.IO

    actual val default: CoroutineDispatcher
        get() = Dispatchers.Default
}
