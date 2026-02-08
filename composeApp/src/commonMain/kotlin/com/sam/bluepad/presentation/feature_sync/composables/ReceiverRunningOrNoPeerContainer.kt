package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    textStyle: TextStyle = MaterialTheme.typography.bodySmallEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentSize()
            ) {
                LoadingIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_empty_title),
                    style = titleStyle,
                    color = titleColor
                )
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_empty_text),
                    style = textStyle,
                    color = textColor
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_empty_box),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_receiver_running_title),
                    style = titleStyle,
                    color = titleColor
                )
                Text(
                    text = stringResource(Res.string.receiver_sync_devices_receiver_running_text),
                    style = textStyle,
                    color = textColor
                )
            }
        }
    }
}