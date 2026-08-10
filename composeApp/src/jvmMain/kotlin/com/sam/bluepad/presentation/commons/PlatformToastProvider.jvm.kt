package com.sam.bluepad.presentation.commons

import kotlin.time.Duration.Companion.milliseconds

actual class PlatformToastProvider(
    val controller: ToastController
) {
    actual suspend fun showToastMessage(message: String, mode: ToastMode) {

        val holdDuration = when (mode) {
            ToastMode.SHORT -> 400.milliseconds
            ToastMode.LONG -> 800.milliseconds
        }
        val message = JVMToastMessage(
            message,
            fadeInDuration = 200.milliseconds,
            holdDuration = holdDuration,
            fadeoutDuration = 120.milliseconds,
        )
        controller.showToast(text = message)
    }
}
