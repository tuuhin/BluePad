package com.sam.bluepad.presentation.utils

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass

val LocalSnackBarState = staticCompositionLocalOf { SnackbarHostState() }

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalWindowSizeInfo = compositionLocalOf { WindowSizeClass(400, 400) }
val LocalPostureInfo = compositionLocalOf { Posture() }

val LocalBluetoothState = compositionLocalOf { false }