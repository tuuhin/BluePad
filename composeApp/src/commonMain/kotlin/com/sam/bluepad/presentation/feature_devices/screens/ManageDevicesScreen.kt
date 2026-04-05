package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_devices.composables.ManageDeviceScreenTopAppBar
import com.sam.bluepad.presentation.feature_devices.composables.SavedExternalDevicesList
import com.sam.bluepad.presentation.feature_devices.events.ManageDevicesScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_add_new_device
import com.sam.bluepad.resources.devices_screen_list_empty
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.resources.ic_no_devices
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDevicesScreen(
    devices: ImmutableList<ExternalDeviceModel>,
    onEvent: (ManageDevicesScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    navigation: @Composable () -> Unit = {},
    onNavigateToAddDeviceRoute: () -> Unit = {},
    onNavigateToAdvertiseRoute: () -> Unit = {},
    onNavigateToRevokeDevicesRoute: () -> Unit = {},
    onNavigateToSyncDeviceRoute: (Uuid) -> Unit = {},
) {
    val snackBarHostState = LocalSnackBarState.current
    val windowSize = LocalWindowSizeInfo.current
    val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val hasAtLeastOneDevice by remember(devices) {
        derivedStateOf { devices.isNotEmpty() }
    }

    val isLargeScreen =
        windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        topBar = {
            ManageDeviceScreenTopAppBar(
                onNavigateToAdvertise = onNavigateToAdvertiseRoute,
                onNavigateToBlockDevices = onNavigateToRevokeDevicesRoute,
                navigation = navigation,
                topBarScrollBehaviour = topBarScrollBehaviour
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasAtLeastOneDevice,
                enter = slideInVertically(),
                exit = slideOutVertically()
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddDeviceRoute,
                    shape = if (isLargeScreen) FloatingActionButtonDefaults.largeExtendedFabShape else FloatingActionButtonDefaults.largeShape,
                    text = { Text(text = stringResource(Res.string.action_add_new_device)) },
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.action_add_new_device),
                            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize),
                        )
                    },
                    expanded = isLargeScreen
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
                EmptyBlackListedDevicesContainer(
                    onAddDevice = onNavigateToAddDeviceRoute,
                    modifier = Modifier.fillMaxSize()
                )
            },
            onItems = { devices ->
                SavedExternalDevicesList(
                    devices = devices,
                    onSyncDevice = { device -> onNavigateToSyncDeviceRoute(device.id) },
                    onRevokeDevice = { device ->
                        onEvent(ManageDevicesScreenEvent.OnRevokeDevice(device))
                    },
                    onDeleteDevice = { device ->
                        onEvent(ManageDevicesScreenEvent.OnDeleteDevice(device))
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
private fun EmptyBlackListedDevicesContainer(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_no_devices),
            contentDescription = "No devices present",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
            modifier = Modifier.size(200.dp)
        )
        Text(
            text = stringResource(Res.string.devices_screen_list_empty),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(200.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ElevatedButton(
            onClick = onAddDevice,
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.shape,
                pressedShape = ButtonDefaults.squareShape
            )
        ) {
            Text(text = stringResource(Res.string.action_add_new_device))
        }
    }
}