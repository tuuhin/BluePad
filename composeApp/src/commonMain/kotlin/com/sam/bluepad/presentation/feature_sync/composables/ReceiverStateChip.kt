package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_running_simple
import com.sam.bluepad.resources.ic_stopped
import com.sam.bluepad.resources.option_receiver_active
import com.sam.bluepad.resources.option_receiver_inactive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiverStateChip(
	isActive: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	SuggestionChip(
		onClick = onClick,
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
		icon = {
			Crossfade(
				targetState = isActive,
				animationSpec = tween(durationMillis = 200, easing = EaseInOut)
			) { isReceiverActive ->
				if (isReceiverActive) Icon(
					painter = painterResource(Res.drawable.ic_running_simple),
					contentDescription = null
				)
				else Icon(
					painter = painterResource(Res.drawable.ic_stopped),
					contentDescription = null
				)
			}
		},
		label = {
			Crossfade(
				targetState = isActive,
				animationSpec = tween(durationMillis = 200, easing = EaseInOut)
			) { isReceiverActive ->
				if (isReceiverActive) Text(text = stringResource(Res.string.option_receiver_active))
				else Text(text = stringResource(Res.string.option_receiver_inactive))
			}
		},
		shape = MaterialTheme.shapes.large,
		colors = SuggestionChipDefaults.suggestionChipColors(
			containerColor = MaterialTheme.colorScheme.tertiaryContainer,
			labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
			iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
		),
		modifier = modifier,
	)
}
