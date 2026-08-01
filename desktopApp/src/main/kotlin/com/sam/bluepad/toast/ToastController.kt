package com.sam.bluepad.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
interface ToastController {
    suspend fun showToast(text: String)
}

class ToastControllerImpl : ToastController {

    private val _showRequests = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val showRequests = _showRequests.asSharedFlow()

    override suspend fun showToast(text: String) {
        _showRequests.emit(text)
    }
}

@Composable
fun rememberToastController(): ToastController = remember { ToastControllerImpl() }
