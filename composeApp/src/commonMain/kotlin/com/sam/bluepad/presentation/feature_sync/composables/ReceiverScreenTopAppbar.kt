package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.receive_sync_devices_list_screen_subtitle
import com.sam.bluepad.resources.receive_sync_devices_list_screen_title
import com.sam.bluepad.resources.turn_off_sync_receiver
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverScreenTopAppbar(
    modifier: Modifier = Modifier,
    scrollBehaviour: TopAppBarScrollBehavior? = null,
    navigation: @Composable () -> Unit = {},
    isSyncRunning: Boolean = false,
    onStopOrCancelSync: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(text = stringResource(Res.string.receive_sync_devices_list_screen_title)) },
        subtitle = { Text(text = stringResource(Res.string.receive_sync_devices_list_screen_subtitle)) },
        actions = {
            AnimatedVisibility(
                visible = isSyncRunning,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut(),
            ) {
                Button(
                    onClick = onStopOrCancelSync,
                    shapes = ButtonDefaults.shapes(
                        shape = ButtonDefaults.shape,
                        pressedShape = ButtonDefaults.mediumPressedShape,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraSmallContainerHeight),
                ) {
                    Text(
                        text = stringResource(Res.string.turn_off_sync_receiver),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        },
        navigationIcon = navigation,
        scrollBehavior = scrollBehaviour,
        modifier = modifier,
    )
}
