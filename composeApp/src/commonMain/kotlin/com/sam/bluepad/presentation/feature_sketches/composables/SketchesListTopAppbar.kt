package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_receive
import com.sam.bluepad.resources.ic_receive
import com.sam.bluepad.resources.sketches_screen_subtitle
import com.sam.bluepad.resources.sketches_screen_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchesListTopAppBar(
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit,
    topBarScrollBehaviour: TopAppBarScrollBehavior? = null,
    onReceiveData: () -> Unit = {},
) {

    val density = LocalDensity.current

    val isExpanded by remember(topBarScrollBehaviour) {
        derivedStateOf {
            val scrollBehavior = topBarScrollBehaviour ?: return@derivedStateOf false
            scrollBehavior.state.collapsedFraction < .2f
        }
    }

    MediumFlexibleTopAppBar(
        title = { Text(text = stringResource(Res.string.sketches_screen_title)) },
        subtitle = {
            AnimatedVisibility(
                visible = isExpanded,
                modifier = Modifier.fillMaxWidth(),
                enter = slideInVertically(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                Text(text = stringResource(Res.string.sketches_screen_subtitle))
            }
        },
        navigationIcon = navigation,
        scrollBehavior = topBarScrollBehaviour,
        modifier = modifier,
        actions = {
            TooltipBox(
                tooltip = {
                    RichTooltip(
                        title = { Text(text = "Receive") },
                        text = { Text("Click to receive from other device") },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TooltipDefaults.richTooltipColors(
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                },
                state = rememberTooltipState(),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Below),
            ) {
                Button(
                    onClick = onReceiveData,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    contentPadding = ButtonDefaults.SmallContentPadding,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_receive),
                        contentDescription = "Receive content",
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(Res.string.action_receive))
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        },
    )
}
