package com.sam.bluepad.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import com.sam.bluepad.theme.BluePadTheme

class BluepadPreviewWrapper : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        BluePadTheme(useSystemFonts = false) {
            content()
        }
    }
}
