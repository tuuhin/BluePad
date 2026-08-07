package com.sam.bluepad.presentation.commons

import android.content.Context
import android.widget.Toast

actual class PlatformToastProvider(private val context: Context) {
    actual suspend fun showToastMessage(message: String, mode: ToastMode) {
        val duration = when (mode) {
            ToastMode.SHORT -> Toast.LENGTH_SHORT
            ToastMode.LONG -> Toast.LENGTH_LONG
        }

        Toast.makeText(context, message, duration)
            .show()
    }
}
