package com.sam.bluepad.common.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Single

@Single(binds = [ICoroutineDispatchersProvider::class])
internal actual class CoroutineDispatcherProvider : ICoroutineDispatchersProvider {


    actual override val main: CoroutineDispatcher
        get() = Dispatchers.Main

    actual override val mainImmediate: CoroutineDispatcher
        get() = Dispatchers.Main.immediate

    actual override val io: CoroutineDispatcher
        get() = Dispatchers.IO

    actual override val default: CoroutineDispatcher
        get() = Dispatchers.Default

    actual override val unconfined: CoroutineDispatcher
        get() = Dispatchers.Unconfined

}
