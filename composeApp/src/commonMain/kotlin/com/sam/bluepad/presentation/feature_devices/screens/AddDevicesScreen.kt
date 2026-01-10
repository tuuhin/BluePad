package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_devices.composables.BLEScanStartStopButton
import com.sam.bluepad.presentation.feature_devices.composables.MultipleDeviceWarning
import com.sam.bluepad.presentation.feature_devices.composables.ScanDeviceList
import com.sam.bluepad.presentation.feature_devices.events.AddDeviceScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.add_devices_screen_subtitle
import com.sam.bluepad.resources.add_devices_screen_title
import com.sam.bluepad.resources.ic_no_devices
import com.sam.bluepad.resources.ic_refresh
import com.sam.bluepad.resources.scan_results_no_device_desc
import com.sam.bluepad.resources.scan_results_no_device_title
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevicesScreen(
	isScanRunning: Boolean,
	searchedPeers: ImmutableList<BLEPeerDevice>,
	onEvent: (AddDeviceScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isListRefreshing: Boolean = false,
	onNavigateToConnect: (String) -> Unit = {},
	onBackNavigation: @Composable () -> Unit = {},
) {

	val lifecycleOwner = LocalLifecycleOwner.current
	val snackBarHostState = LocalSnackBarState.current

	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	LifecycleResumeEffect(lifecycleOwner = lifecycleOwner, key1 = Unit) {
		onPauseOrDispose {
			// stop the scan if we go to background
			onEvent(AddDeviceScreenEvent.OnStopDeviceScan)
		}
	}

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.add_devices_screen_title)) },
				subtitle = { Text(text = stringResource(Res.string.add_devices_screen_subtitle)) },
				navigationIcon = onBackNavigation,
				scrollBehavior = topBarScrollBehaviour,
				actions = {
					OutlinedButton(
						onClick = { onEvent(AddDeviceScreenEvent.OnRefreshDeviceList) },
						enabled = !isListRefreshing && !isScanRunning,
						contentPadding = ButtonDefaults.SmallContentPadding,
					) {
						Icon(
							painter = painterResource(Res.drawable.ic_refresh),
							contentDescription = null
						)
					}
					Spacer(modifier = Modifier.width(4.dp))
					BLEScanStartStopButton(
						isScanning = isScanRunning,
						onStopScan = { onEvent(AddDeviceScreenEvent.OnStopDeviceScan) },
						onStartScan = { onEvent(AddDeviceScreenEvent.OnStartDeviceScan) },
						modifier = Modifier.padding(end = 4.dp)
					)
				}
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		Column(
			modifier = Modifier.padding(padding),
			verticalArrangement = Arrangement.spacedBy(2.dp),
		) {
			AnimatedVisibility(
				visible = isScanRunning,
				enter = fadeIn() + slideInHorizontally(),
				exit = fadeOut() + slideOutHorizontally(),
				modifier = Modifier.fillMaxWidth()
			) {
				LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
			}
			MultipleDeviceWarning(
				showWarning = isScanRunning,
				modifier = Modifier
					.fillMaxWidth(.8f)
					.align(Alignment.CenterHorizontally)
			)
			ListContentLoadingWrapper(
				content = searchedPeers,
				onItems = { peers ->
					ScanDeviceList(
						searchedPeers = peers,
						isListRefreshing = isListRefreshing,
						onListRefresh = { onEvent(AddDeviceScreenEvent.OnRefreshDeviceList) },
						onConnect = { device ->
							onNavigateToConnect(device.deviceAddress)
							if (isScanRunning) onEvent(AddDeviceScreenEvent.OnStopDeviceScan)
						},
						modifier = Modifier.fillMaxSize(),
					)
				},
				onEmpty = {
					NoDevicesFoundContainer(modifier = Modifier.fillMaxSize())
				},
				modifier = Modifier.weight(1f)
					.animateContentSize(MaterialTheme.motionScheme.fastEffectsSpec())
					.padding(
						horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
						vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
					)
			)
		}
	}

}

@Composable
private fun NoDevicesFoundContainer(modifier: Modifier = Modifier) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
		modifier = modifier,
	) {
		Image(
			painter = painterResource(Res.drawable.ic_no_devices),
			contentDescription = "No devices present",
			modifier = Modifier.size(64.dp),
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
		)
		Spacer(modifier = Modifier.height(24.dp))
		Text(
			text = stringResource(Res.string.scan_results_no_device_title),
			style = MaterialTheme.typography.headlineSmallEmphasized,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = stringResource(Res.string.scan_results_no_device_desc),
			style = MaterialTheme.typography.bodyMediumEmphasized,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
		)
	}
}