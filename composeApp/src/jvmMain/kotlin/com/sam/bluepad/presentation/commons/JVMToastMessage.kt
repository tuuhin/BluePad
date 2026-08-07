package com.sam.bluepad.presentation.commons

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class JVMToastMessage(
    val message: String,
    val fadeInDuration: Duration = 200.milliseconds,
    val holdDuration: Duration = 1200.milliseconds,
    val fadeoutDuration: Duration = 120.milliseconds
)
