package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_devices.composables.BlackListDeviceScreenTopAppbar
import com.sam.bluepad.presentation.feature_devices.composables.BlackListedDevicesList
import com.sam.bluepad.presentation.feature_devices.events.BlackListDeviceScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.black_list_devices_no_items_title
import com.sam.bluepad.resources.ic_devices_alternate
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackListedDevicesListScreen(
	devices: ImmutableList<ExternalDeviceModel>,
	onEvent: (BlackListDeviceScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	isLoading: Boolean = false,
) {
	val snackBarHostState = LocalSnackBarState.current
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	Scaffold(
		topBar = {
			BlackListDeviceScreenTopAppbar(
				onUnRevokeAll = { onEvent(BlackListDeviceScreenEvent.OnRestoreAllDevice) },
				navigation = navigation,
				topBarScrollBehaviour = topBarScrollBehaviour
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		ListContentLoadingWrapper(
			content = devices,
			isLoading = isLoading,
			modifier = Modifier.fillMaxSize().padding(padding),
			onEmpty = {
				EmptyBlackListedDevicesContainer(modifier = Modifier.fillMaxSize())
			},
			onItems = { devices ->
				BlackListedDevicesList(
					devices = devices,
					onUnRevoke = { device ->
						onEvent(BlackListDeviceScreenEvent.OnRestoreDevice(device))
					},
					onDeleteDevice = { device ->
						onEvent(BlackListDeviceScreenEvent.OnDeleteDevice(device))
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

@Composable
private fun EmptyBlackListedDevicesContainer(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(Res.drawable.ic_devices_alternate),
			contentDescription = "No devices present",
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
			modifier = Modifier.size(200.dp)
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(Res.string.black_list_devices_no_items_title),
			style = MaterialTheme.typography.bodyMediumEmphasized,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center,
			modifier = Modifier.width(200.dp),
		)

	}
}