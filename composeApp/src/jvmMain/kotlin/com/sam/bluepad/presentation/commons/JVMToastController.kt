package com.sam.bluepad.presentation.commons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
interface ToastController {
    val requests: SharedFlow<JVMToastMessage>
    suspend fun showToast(text: JVMToastMessage)
}

internal class ToastControllerImpl : ToastController {

    private val _showRequests = MutableSharedFlow<JVMToastMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val requests: SharedFlow<JVMToastMessage> = _showRequests.asSharedFlow()
    override suspend fun showToast(text: JVMToastMessage) {
        _showRequests.emit(text)
    }
}

@Composable
fun rememberToastController(): ToastController = remember { ToastControllerImpl() }
