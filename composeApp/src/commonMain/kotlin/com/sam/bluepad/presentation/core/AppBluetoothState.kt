package com.sam.bluepad.presentation.core

data class AppBluetoothState(
    val isBTActive: Boolean = false,
    val canRequestBTActive: Boolean = false,
    val canOpenBTSettings: Boolean = false
)
