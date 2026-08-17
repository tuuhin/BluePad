package com.sam.bluepad.data.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CoroutineLifecycleOwner(context: CoroutineContext) : LifecycleOwner {

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.CREATED
        }

    init {
        lifecycle.currentState = Lifecycle.State.STARTED
        val job = context[Job]
        if (job == null || job.isCancelled) {
            markDestroyed()
        } else {
            job.invokeOnCompletion {
                markDestroyed()
            }
        }
    }

    private fun markDestroyed() {
        val mainDispatcher = Dispatchers.Main.immediate
        CoroutineScope(mainDispatcher).launch {
            lifecycle.currentState = Lifecycle.State.DESTROYED
        }
    }
}
