package com.sam.bluepad.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import dev.nucleusframework.window.tao.LocalTaoWindow
import org.koin.compose.koinInject

@Composable
fun SetWindowHandleShareSheet() {

    val window = LocalTaoWindow.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val shareSheetController = koinInject<ShareSketchInteraction>()

    DisposableEffect(window) {

        val eventObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // TODO: AWT BACKEND IS NOT TESTED
                    val windowHandle = window?.nativeHandle ?: 0L
                    // set the window handle
                    shareSheetController.setWindowHandle(windowHandle)
                }

                // reset the window handle
                Lifecycle.Event.ON_STOP -> shareSheetController.setWindowHandle(-1L)

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(eventObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(eventObserver)
        }
    }
}
