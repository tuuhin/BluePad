package com.sam.bluepad.presentation.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.state_loading
import org.jetbrains.compose.resources.stringResource

private enum class ContentState {
    SUCCESS,
    LOADING,
    ERROR,
}

@Composable
fun <T> ContentLoadingWrapper(
    content: T?,
    isLoading: Boolean,
    onSuccess: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    onFailed: @Composable () -> Unit = {},
    isContentLoadFailed: Boolean = false,
    onLoading: (@Composable () -> Unit)? = null,
) {
    val loadState by remember(content, isLoading) {
        derivedStateOf {
            if (isLoading) ContentState.LOADING
            else if (content != null && !isContentLoadFailed) ContentState.SUCCESS
            else ContentState.ERROR
        }
    }

    val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    AnimatedContent(
        targetState = loadState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = animationSpec) togetherWith
                fadeOut(animationSpec = animationSpec) using
                SizeTransform(clip = false)
        },
        contentAlignment = Alignment.Center,
    ) { current ->
        when (current) {
            ContentState.ERROR -> onFailed.invoke()

            ContentState.LOADING -> {
                onLoading?.invoke() ?: Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.state_loading),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            ContentState.SUCCESS -> content?.let { onSuccess.invoke(it) }
        }
    }
}
