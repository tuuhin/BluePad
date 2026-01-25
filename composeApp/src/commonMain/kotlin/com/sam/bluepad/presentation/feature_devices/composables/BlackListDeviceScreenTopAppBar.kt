package com.sam.bluepad.presentation.feature_devices.composables

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
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.black_list_devices_screen_action_restore_all
import com.sam.bluepad.resources.black_list_devices_screen_text
import com.sam.bluepad.resources.black_list_devices_screen_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackListDeviceScreenTopAppbar(
	onUnRevokeAll: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit,
	topBarScrollBehaviour: TopAppBarScrollBehavior? = null
) {

	MediumFlexibleTopAppBar(
		title = { Text(text = stringResource(Res.string.black_list_devices_screen_title)) },
		subtitle = { Text(text = stringResource(Res.string.black_list_devices_screen_text)) },
		navigationIcon = navigation,
		scrollBehavior = topBarScrollBehaviour,
		actions = {
			Button(
				onClick = onUnRevokeAll,
				shapes = ButtonDefaults.shapes(),
				contentPadding = ButtonDefaults.SmallContentPadding,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer
				)
			) {
				Text(text = stringResource(Res.string.black_list_devices_screen_action_restore_all))
			}
			Spacer(modifier = Modifier.width(4.dp))
		},
		modifier = modifier,
	)
}