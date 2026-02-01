package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_settings.SettingsScreen
import com.sam.bluepad.presentation.feature_settings.SettingsViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.settingsRouteEntry(
    backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.SettingsRoute> {

    val viewmodel = koinViewModel<SettingsViewmodel>()
    val currentDevice by viewmodel.localDeviceData.collectAsStateWithLifecycle()

    UiEventsHandler(eventsFlow = viewmodel::uiEvent)

    SettingsScreen(deviceState = currentDevice, onEvent = viewmodel::onEvent)
}