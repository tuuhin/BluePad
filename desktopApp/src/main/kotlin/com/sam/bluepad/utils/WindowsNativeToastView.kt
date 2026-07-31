package com.sam.bluepad.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.sam.bluepad.toast.ToastController
import com.sam.bluepad.toast.ToastControllerImpl
import com.sam.bluepad.toast.WindowsNativeToastView
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.window.tao.LocalTaoWindow
import dev.nucleusframework.window.tao.NativeView

@Composable
internal fun NativeToastProvider(
    controller: ToastController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val taoWindow = LocalTaoWindow.current
    val surfaceColor = MaterialTheme.colorScheme.primaryContainer

    val factoryContent = remember(taoWindow) {
        if (Platform.Current == Platform.Windows) {
            WindowsNativeToastView(taoWindow?.nativeHandle ?: 0L)
        } else throw IllegalArgumentException("Invalid target")
    }

    LaunchedEffect(controller, factoryContent) {
        if (controller is ToastControllerImpl) {
            controller.showRequests.collect { text ->
                factoryContent.show(text)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        content()
        NativeView(
            factory = { factoryContent },
            update = { view ->
                if (view is WindowsNativeToastView) {
                    view.setBackground(color = surfaceColor.toArgb())
                }
            },
            cornerRadius = 12.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .offset(y = ((-20).dp))
                .sizeIn(minWidth = 180.dp, minHeight = 32.dp),
        )
    }
}
