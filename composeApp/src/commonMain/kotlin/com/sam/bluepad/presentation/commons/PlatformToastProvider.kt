package com.sam.bluepad.presentation.commons


expect class PlatformToastProvider {

    suspend fun showToastMessage(message: String, mode: ToastMode = ToastMode.SHORT)
}
