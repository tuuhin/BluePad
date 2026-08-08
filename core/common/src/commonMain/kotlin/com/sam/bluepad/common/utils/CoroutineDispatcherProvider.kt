package com.sam.bluepad.common.utils

import kotlinx.coroutines.CoroutineDispatcher

internal expect class CoroutineDispatcherProvider : ICoroutineDispatchersProvider {


    override val main: CoroutineDispatcher
    override val mainImmediate: CoroutineDispatcher
    override val io: CoroutineDispatcher
    override val default: CoroutineDispatcher
    override val unconfined: CoroutineDispatcher
}
