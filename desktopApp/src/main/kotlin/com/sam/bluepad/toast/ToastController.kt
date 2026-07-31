package com.sam.bluepad.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@Stable
interface ToastController {
    fun showToast(text: String)
}

class ToastControllerImpl : ToastController {
    internal val showRequests = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun showToast(text: String) {
        showRequests.tryEmit(text)
    }
}

@Composable
fun rememberToastController(): ToastController {
    return remember { ToastControllerImpl() }
}
