package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_devices.composables.EmptyDevicesListContainer
import com.sam.bluepad.presentation.feature_devices.composables.SavedExternalDevicesList
import com.sam.bluepad.presentation.feature_devices.events.ManageDevicesScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_ble_advertise
import com.sam.bluepad.resources.devices_screen_subtitle
import com.sam.bluepad.resources.devices_screen_title
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDevicesScreen(
	devices: ImmutableList<ExternalDeviceModel>,
	onEvent: (ManageDevicesScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	navigation: @Composable () -> Unit = {},
	onNavigateToAddDevice: () -> Unit = {},
	onNavigateToAdvertise: () -> Unit = {},
) {
	val snackBarHostState = LocalSnackBarState.current
	val windowSize = LocalWindowSizeInfo.current
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	val hasAtLeastOneDevice by remember(devices) {
		derivedStateOf { devices.isNotEmpty() }
	}

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.devices_screen_title)) },
				subtitle = { Text(text = stringResource(Res.string.devices_screen_subtitle)) },
				navigationIcon = navigation,
				scrollBehavior = topBarScrollBehaviour,
				actions = {
					TextButton(
						onClick = onNavigateToAdvertise,
						modifier = Modifier.offset((-10).dp)
					) {
						Text(stringResource(Res.string.action_ble_advertise))
					}
				}
			)
		},
		floatingActionButton = {
			AnimatedVisibility(
				visible = hasAtLeastOneDevice,
				enter = slideInVertically(),
				exit = slideOutVertically()
			) {
				ExtendedFloatingActionButton(
					onClick = onNavigateToAddDevice,
					text = { Text(text = "Add") },
					icon = {
						Icon(
							painter = painterResource(Res.drawable.ic_add),
							contentDescription = "Add"
						)
					},
					expanded = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
				)
			}
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		ListContentLoadingWrapper(
			content = devices,
			isLoading = isLoading,
			modifier = Modifier.fillMaxSize().padding(padding),
			onEmpty = {
				EmptyDevicesListContainer(
					onAddDevice = onNavigateToAddDevice,
					modifier = Modifier.fillMaxSize()
				)
			},
			onItems = { devices ->
				SavedExternalDevicesList(
					devices = devices,
					onSyncDevice = { device -> onEvent(ManageDevicesScreenEvent.OnSyncDevice(device)) },
					onRevokeDevice = { device ->
						onEvent(ManageDevicesScreenEvent.OnRevokeDevice(device))
					},
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(
						horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
						vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
					)
				)
			},
		)
	}
}