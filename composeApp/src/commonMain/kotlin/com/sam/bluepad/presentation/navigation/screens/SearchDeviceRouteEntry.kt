package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.events.ScanDevicesNavEvent
import com.sam.bluepad.presentation.feature_devices.screens.AddDevicesScreen
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEScanDevicesViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph.ConnectDeviceRoute
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph.CreateDeviceBondRoute
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.searchDevicesEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SearchDeviceRoute> {

    val viewModel = koinViewModel<BLEScanDevicesViewModel>()
    val screenState by viewModel.state.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is ScanDevicesNavEvent.NavigateToConnect ->
                        // navigate to connect route
                        backStack.add(ConnectDeviceRoute(event.device.deviceAddress))

                    is ScanDevicesNavEvent.NavigateToCreateBond ->
                        // navigate to bond route
                        backStack.add(CreateDeviceBondRoute(event.device.deviceAddress, event.device.bleDeviceName))
                }
            }
        }
    }

    UiEventsHandler(eventsFlow = viewModel::uiEvent)

    // an extra dialog for handling the bond state


    AddDevicesScreen(
        state = screenState,
        onEvent = viewModel::onEvent,
        onBackNavigation = {
            if (backStack.isNotEmpty()) {
                IconButton(onClick = { backStack.removeLastOrNull() }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
            }
        },
    )
}
