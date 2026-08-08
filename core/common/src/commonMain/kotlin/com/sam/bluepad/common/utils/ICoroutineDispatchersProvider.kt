package com.sam.bluepad.common.utils

import kotlinx.coroutines.CoroutineDispatcher

interface ICoroutineDispatchersProvider {


    val main: CoroutineDispatcher
    val mainImmediate: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
