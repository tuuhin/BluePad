package com.sam.bluepad.presentation.composables

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.state_loading
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

private enum class ListLoadingState {
	EMPTY,
	LOADING,
	CONTENT,
}

@Composable
fun <T> ListContentLoadingWrapper(
	content: ImmutableList<T>,
	onItems: @Composable (ImmutableList<T>) -> Unit,
	onEmpty: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	onLoading: (@Composable () -> Unit)? = null,
) {

	val loadState = remember(content, isLoading) {
		if (isLoading) ListLoadingState.LOADING
		else if (content.isEmpty()) ListLoadingState.EMPTY
		else ListLoadingState.CONTENT
	}

	Crossfade(
		targetState = loadState,
		modifier = modifier,
		animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
	) { current ->
		when (current) {
			ListLoadingState.EMPTY -> onEmpty.invoke()
			ListLoadingState.LOADING -> {
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
						color = MaterialTheme.colorScheme.primary
					)
				}
			}

			ListLoadingState.CONTENT -> onItems.invoke(content)
		}
	}
}