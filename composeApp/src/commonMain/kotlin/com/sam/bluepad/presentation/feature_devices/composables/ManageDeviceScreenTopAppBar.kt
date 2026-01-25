package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_blacklist_devices_screen
import com.sam.bluepad.resources.action_ble_advertise
import com.sam.bluepad.resources.devices_screen_subtitle
import com.sam.bluepad.resources.devices_screen_title
import com.sam.bluepad.resources.ic_vert_menu
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDeviceScreenTopAppBar(
	onNavigateToAdvertise: () -> Unit,
	onNavigateToBlockDevices: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	topBarScrollBehaviour: TopAppBarScrollBehavior? = null,
) {

	var isExpanded by remember { mutableStateOf(false) }

	MediumFlexibleTopAppBar(
		title = { Text(text = stringResource(Res.string.devices_screen_title)) },
		subtitle = { Text(text = stringResource(Res.string.devices_screen_subtitle)) },
		navigationIcon = navigation,
		scrollBehavior = topBarScrollBehaviour,
		actions = {
			SplitButtonLayout(
				leadingButton = {
					SplitButtonDefaults.LeadingButton(
						onClick = onNavigateToAdvertise,
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							contentColor = MaterialTheme.colorScheme.onSecondaryContainer
						),
						contentPadding = SplitButtonDefaults.SmallLeadingButtonContentPadding,
					) {
						Text(text = stringResource(Res.string.action_ble_advertise))
					}
				},
				trailingButton = {
					Box {
						SplitButtonDefaults.TrailingButton(
							checked = isExpanded,
							onCheckedChange = { isExpanded = !isExpanded },
							colors = ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.tertiaryContainer,
								contentColor = MaterialTheme.colorScheme.onTertiaryContainer
							),
							contentPadding = SplitButtonDefaults.SmallTrailingButtonContentPadding,
						) {
							Icon(
								painter = painterResource(Res.drawable.ic_vert_menu),
								contentDescription = "Options"
							)
						}
						DropdownMenu(
							expanded = isExpanded,
							onDismissRequest = { isExpanded = false },
							shape = MaterialTheme.shapes.medium
						) {
							DropdownMenuItem(
								text = { Text(text = stringResource(Res.string.action_blacklist_devices_screen)) },
								onClick = onNavigateToBlockDevices,
							)
						}
					}
				},
			)
			Spacer(modifier = Modifier.width(4.dp))
		},
		modifier = modifier,
	)
}