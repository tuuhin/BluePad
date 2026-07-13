package com.sam.bluepad.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.sam.bluepad.desktop.resources.Res
import com.sam.bluepad.desktop.resources.app_name
import com.sam.bluepad.desktop.resources.ic_notepad
import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.material.MaterialTitleBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NucleusApplicationScope.NucleusWindowWrapper(
    content: @Composable () -> Unit
) {
    val windowState = rememberWindowState(position = WindowPosition(Alignment.Center))

    MaterialDecoratedWindow(
        onCloseRequest = ::exitApplication,
        icon = painterResource(Res.drawable.ic_notepad),
        title = stringResource(Res.string.app_name),
        state = windowState,
    ) {
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
