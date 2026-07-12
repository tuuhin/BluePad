package com.sam.bluepad.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.sam.bluepad.desktop.resources.Res
import com.sam.bluepad.desktop.resources.app_name
import com.sam.bluepad.desktop.resources.ic_notepad
import io.github.kdroidfilter.nucleus.window.material.MaterialDecoratedWindow
import io.github.kdroidfilter.nucleus.window.material.MaterialTitleBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension

@Composable
internal fun ApplicationScope.NucleusWindowWrapper(
    minSize: DpSize = DpSize(360.dp, 540.dp),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val windowState = rememberWindowState(position = WindowPosition(Alignment.Center))

    MaterialDecoratedWindow(
        onCloseRequest = ::exitApplication,
        icon = painterResource(Res.drawable.ic_notepad),
        title = stringResource(Res.string.app_name),
        state = windowState,
    ) {
        // min window size
        window.minimumSize = with(density) {
            Dimension(
                minSize.width.roundToPx(),
                minSize.height.roundToPx(),
            )
        }
        // title bar
        MaterialTitleBar {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        //Your app content
        content()
    }
}
