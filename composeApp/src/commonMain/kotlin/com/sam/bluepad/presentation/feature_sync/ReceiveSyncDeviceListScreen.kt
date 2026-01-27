package com.sam.bluepad.presentation.feature_sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.feature_sync.composables.ReceiveDevicesOptionsList
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverStateChip
import com.sam.bluepad.presentation.feature_sync.event.ReceiveDeviceSyncScreenEvents
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_continue
import com.sam.bluepad.resources.receive_sync_devices_list_screen_subtitle
import com.sam.bluepad.resources.receive_sync_devices_list_screen_title
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveSyncDeviceListScreen(
	devicesList: ImmutableList<ExternalDeviceModel>,
	isAdvertising: Boolean,
	onEvent: (ReceiveDeviceSyncScreenEvents) -> Unit,
	modifier: Modifier = Modifier,
	selectedDevice: ExternalDeviceModel? = null,
	navigation: @Composable () -> Unit = {},
	onConfirmSelectedDevice: (ExternalDeviceModel) -> Unit = {},
) {
	val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.receive_sync_devices_list_screen_title)) },
				subtitle = { Text(text = stringResource(Res.string.receive_sync_devices_list_screen_subtitle)) },
				navigationIcon = navigation,
				scrollBehavior = scrollBehaviour,
				actions = {
					AnimatedVisibility(
						visible = selectedDevice != null,
						enter = slideInHorizontally() + fadeIn(),
						exit = slideOutHorizontally() + fadeOut()
					) {
						Button(
							onClick = { selectedDevice?.let { onConfirmSelectedDevice(it) } }
						) {
							Text(stringResource(Res.string.action_continue))
						}
					}
				}
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
	) { scPadding ->
		Box(
			modifier = Modifier.fillMaxSize().padding(
				horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
				vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
			),
		) {
			ReceiverStateChip(
				isActive = isAdvertising,
				onClick = { onEvent(ReceiveDeviceSyncScreenEvents.ToggleReceiver) },
				modifier = Modifier.align(Alignment.BottomCenter)
			)
			ReceiveDevicesOptionsList(
				devicesList = devicesList,
				isAdvertising = isAdvertising,
				onSelectDevice = { device ->
					onEvent(
						ReceiveDeviceSyncScreenEvents.OnSelectDevice(
							device
						)
					)
				},
				paddingValues = scPadding,
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}