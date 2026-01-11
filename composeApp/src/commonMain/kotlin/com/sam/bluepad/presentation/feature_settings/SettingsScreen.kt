package com.sam.bluepad.presentation.feature_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_settings.composable.UpdateDeviceNameListItem
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_re_enroll_device
import com.sam.bluepad.resources.settings_screen_re_enroll_device_text
import com.sam.bluepad.resources.settings_screen_re_enroll_device_title
import com.sam.bluepad.resources.settings_screen_subtitle
import com.sam.bluepad.resources.settings_screen_title
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	state: SettingsScreenState,
	onEvent: (SettingsScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	navigation: @Composable () -> Unit = {}
) {
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.settings_screen_title)) },
				subtitle = { Text(text = stringResource(Res.string.settings_screen_subtitle)) },
				navigationIcon = navigation,
				scrollBehavior = topBarScrollBehaviour
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		ContentLoadingWrapper(
			content = state,
			isLoading = isLoading,
			modifier = Modifier.padding(padding),
			onSuccess = {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(300.dp),
					contentPadding = PaddingValues(
						vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
						horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING
					),
					verticalArrangement = Arrangement.spacedBy(6.dp),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
					modifier = Modifier.fillMaxSize(),
				) {
					item {
						UpdateDeviceNameListItem(
							currentName = state.deviceName,
							onUpdateName = { newName ->
								onEvent(SettingsScreenEvent.OnUpdateDeviceName(newName))
							},
							modifier = Modifier.animateItem(),
						)
					}
					item {
						ListItem(
							headlineContent = { Text(text = stringResource(Res.string.settings_screen_re_enroll_device_title)) },
							supportingContent = { Text(text = stringResource(Res.string.settings_screen_re_enroll_device_text)) },
							trailingContent = {
								Button(
									onClick = { onEvent(SettingsScreenEvent.OnReEnrollRevokeDevices) },
									colors = ButtonDefaults.buttonColors(
										containerColor = MaterialTheme.colorScheme.tertiaryContainer,
										contentColor = MaterialTheme.colorScheme.onTertiaryContainer
									)
								) {
									Text(text = stringResource(Res.string.action_re_enroll_device))
								}
							},
							colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
							modifier = modifier.clip(MaterialTheme.shapes.medium),
						)
					}
				}
			},
			onFailed = {},
		)
	}
}