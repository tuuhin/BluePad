package com.sam.bluepad.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.commons.ToastController
import com.sam.bluepad.presentation.commons.rememberToastController
import com.sam.bluepad.utils.MacOSXNativeToastView
import com.sam.bluepad.utils.WindowsNativeToastView
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.window.tao.LocalTaoWindow
import dev.nucleusframework.window.tao.NativeView

@Composable
internal fun NativeToastProvider(
    modifier: Modifier = Modifier,
    controller: ToastController = rememberToastController(),
    alignment: Alignment = Alignment.BottomCenter,
    offset: () -> DpOffset = { DpOffset(x = 0.dp, y = (-10).dp) },
    content: @Composable BoxScope.() -> Unit,
) {
    val taoWindow = LocalTaoWindow.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow

    val factoryContent = remember(taoWindow) {
        val parentHandle = taoWindow?.nativeHandle ?: 0L
        when (Platform.Current) {
            Platform.Windows -> WindowsNativeToastView(parentHandle)
            Platform.MacOS -> MacOSXNativeToastView(parentHandle)
            else -> throw IllegalArgumentException("Invalid target")
        }
    }

    LaunchedEffect(controller, factoryContent) {
        controller.requests.collect { toastMessage ->
            when (factoryContent) {
                is WindowsNativeToastView -> factoryContent.show(
                    text = toastMessage.message,
                    holdDuration = toastMessage.holdDuration,
                    fadeoutMs = toastMessage.fadeoutDuration,
                    fadeInDuration = toastMessage.fadeInDuration,
                )

                is MacOSXNativeToastView -> factoryContent.show(
                    text = toastMessage.message,
                    holdDuration = toastMessage.holdDuration,
                    fadeoutMs = toastMessage.fadeoutDuration,
                    fadeInDuration = toastMessage.fadeInDuration,
                )

                else -> {}
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = alignment,
    ) {
        // main view
        content()
        // toast view
        NativeView(
            factory = { factoryContent },
            update = { view ->
                if (view is WindowsNativeToastView) {
                    view.setBackground(color = surfaceColor.toArgb())
                }
            },
            cornerRadius = 12.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .sizeIn(minWidth = 180.dp, minHeight = 40.dp)
                .offset {
                    val dpOffset = offset()
                    IntOffset(dpOffset.x.roundToPx(), dpOffset.y.roundToPx())
                },
        )
    }
}
