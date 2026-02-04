package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_empty_box
import com.sam.bluepad.resources.receiver_sync_devices_empty_text
import com.sam.bluepad.resources.receiver_sync_devices_empty_title
import com.sam.bluepad.resources.receiver_sync_devices_receiver_running_text
import com.sam.bluepad.resources.receiver_sync_devices_receiver_running_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiverRunningOrNoPeerContainer(
    modifier: Modifier,
    isRunning: Boolean
) {
    val motionScheme = MaterialTheme.LocalMotionScheme.current

    AnimatedContent(
        targetState = isRunning,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            scaleIn(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.slowEffectsSpec()) togetherWith
                    shrinkOut(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.slowEffectsSpec())
        }
    ) { isActive ->
        if (isActive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize()
            ) {
                LoadingIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_receiver_running_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_receiver_running_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_empty_box),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_empty_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}