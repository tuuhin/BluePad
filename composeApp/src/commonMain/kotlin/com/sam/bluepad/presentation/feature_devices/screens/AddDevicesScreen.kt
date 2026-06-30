package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.WavyProgressIndicatorDefaults
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_devices.composables.BLEScanStartStopButton
import com.sam.bluepad.presentation.feature_devices.composables.MultipleDeviceWarning
import com.sam.bluepad.presentation.feature_devices.composables.ScanDevicesList
import com.sam.bluepad.presentation.feature_devices.events.AddDeviceScreenEvent
import com.sam.bluepad.presentation.feature_devices.state.AddDeviceScreenState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.add_devices_screen_subtitle
import com.sam.bluepad.resources.add_devices_screen_title
import com.sam.bluepad.resources.ic_no_devices
import com.sam.bluepad.resources.ic_refresh
import com.sam.bluepad.resources.scan_results_no_device_desc
import com.sam.bluepad.resources.scan_results_no_device_title
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevicesScreen(
    state: AddDeviceScreenState,
    onEvent: (AddDeviceScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
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

    val isRefreshButtonEnabled by remember(state) {
        derivedStateOf { state.isRefreshButtonEnabled }
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
                        enabled = isRefreshButtonEnabled,
                        contentPadding = ButtonDefaults.SmallContentPadding,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_refresh),
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    BLEScanStartStopButton(
                        isScanning = state.isScanning,
                        onStopScan = { onEvent(AddDeviceScreenEvent.OnStopDeviceScan) },
                        onStartScan = { onEvent(AddDeviceScreenEvent.OnStartDeviceScan) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AnimatedVisibility(
                visible = state.isScanning,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LinearWavyProgressIndicator(
                    wavelength = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier.weight(1f)
                    .fillMaxSize()
                    .padding(
                        horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                        vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
                    ),
            ) {
                MultipleDeviceWarning(
                    showWarning = state.isScanning,
                    modifier = Modifier.fillMaxWidth(.75f)
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                )
                ListContentLoadingWrapper(
                    content = state.peers,
                    onItems = { peers ->
                        ScanDevicesList(
                            searchedPeers = peers,
                            isListRefreshing = state.isListRefreshing,
                            onListRefresh = { onEvent(AddDeviceScreenEvent.OnRefreshDeviceList) },
                            onConnect = { device ->
                                // stop running scan before connect performing this for all cases
                                if (state.isScanning) onEvent(AddDeviceScreenEvent.OnStopDeviceScan)
                                onEvent(AddDeviceScreenEvent.CheckBondStateForDevice(device))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    onEmpty = {
                        NoDevicesFoundContainer(
                            showContainer = !state.isScanning,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun NoDevicesFoundContainer(
    showContainer: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showContainer,
        enter = scaleIn(
            initialScale = .75f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = .4f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        ) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_no_devices),
                contentDescription = "No devices present",
                modifier = Modifier.size(72.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.scan_results_no_device_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
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
}
