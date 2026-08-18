package com.sam.bluepad.presentation.utils

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.platform.PlatformDeviceInfo

val LocalSnackBarState = staticCompositionLocalOf { SnackbarHostState() }

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedContentScope = staticCompositionLocalOf<AnimatedContentScope?> { null }
val LocalPlatformDetails = staticCompositionLocalOf<PlatformDeviceInfo> { PlatformDeviceInfo() }

val LocalWindowSizeInfo = compositionLocalOf { WindowSizeClass(400, 400) }
val LocalPostureInfo = compositionLocalOf { Posture() }
