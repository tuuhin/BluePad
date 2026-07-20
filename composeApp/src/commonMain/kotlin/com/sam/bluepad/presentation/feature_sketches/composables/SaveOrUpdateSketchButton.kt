package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.presentation.utils.LocalAnimatedContentScope
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.presentation.utils.transitions.sharedTransitionRenderInOverlay
import com.sam.bluepad.presentation.utils.transitions.sharedTransitionSkipChildPosition
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_save
import com.sam.bluepad.resources.action_update
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.resources.ic_sketch_update
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SaveOrUpdateSketchButton(
    onUpdateSketch: () -> Unit,
    onSaveSketch: () -> Unit,
    modifier: Modifier = Modifier,
    isNewContent: Boolean = true,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {

    val contentScope = LocalAnimatedContentScope.current
    val windowSize = LocalWindowSizeInfo.current

    val isLargeScreen = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val isTransitionRunning = contentScope?.transition?.isRunning ?: false
    val isButtonClickable = enabled && !isTransitionRunning

    ExtendedFloatingActionButton(
        onClick = {
            if (isButtonClickable) {
                if (isNewContent) onSaveSketch()
                else onUpdateSketch()
            }
        },
        text = {
            Text(
                text = if (isNewContent) stringResource(Res.string.action_save)
                else stringResource(Res.string.action_update),
            )
        },
        icon = {
            Icon(
                painter = if (isNewContent)
                    painterResource(Res.drawable.ic_add)
                else painterResource(Res.drawable.ic_sketch_update),
                contentDescription = "Save Action",
            )
        },
        shape = if (isLargeScreen) FloatingActionButtonDefaults.largeExtendedFabShape
        else FloatingActionButtonDefaults.largeShape,
        expanded = isLargeScreen,
        interactionSource = interactionSource,
        modifier = modifier.then(
            if (contentScope == null) Modifier
            else with(contentScope) {
                Modifier
                    .sharedTransitionRenderInOverlay(1f)
                    .sharedTransitionSkipChildPosition()
                    .animateEnterExit(
                        enter = slideInVertically(
                            animationSpec = tween(durationMillis = 350, easing = EaseOutBack),
                            initialOffsetY = { fullHeight -> fullHeight * 2 },
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200),
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(durationMillis = 250, easing = EaseInCubic),
                            targetOffsetY = { fullHeight -> fullHeight },
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 150),
                        ),
                    )
            },
        ),
    )
}
