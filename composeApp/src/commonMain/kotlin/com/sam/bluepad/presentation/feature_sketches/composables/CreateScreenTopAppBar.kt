package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_delete
import com.sam.bluepad.resources.ic_delete
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreenTopAppBar(
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit,
	topBarScrollBehaviour: TopAppBarScrollBehavior? = null,
	showDeleteAction: Boolean = false,
	showActions: Boolean = false,
	onDelete: () -> Unit = {},
) {
	TopAppBar(
		title = { },
		navigationIcon = navigation,
		scrollBehavior = topBarScrollBehaviour,
		modifier = modifier,
		actions = {
			AnimatedVisibility(
				visible = showActions,
				enter = slideInHorizontally(MaterialTheme.motionScheme.slowEffectsSpec()) + fadeIn(),
				exit = slideOutHorizontally(MaterialTheme.motionScheme.slowEffectsSpec()) + fadeOut(),
				modifier = Modifier.offset(x = (-20).dp)
			) {
				Button(
					onClick = onDelete,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentColor = MaterialTheme.colorScheme.onErrorContainer
					),
					enabled = showDeleteAction,
					contentPadding = ButtonDefaults.SmallContentPadding,
				) {
					Icon(
						painter = painterResource(Res.drawable.ic_delete),
						contentDescription = stringResource(Res.string.action_delete)
					)
				}
			}
		}
	)
}