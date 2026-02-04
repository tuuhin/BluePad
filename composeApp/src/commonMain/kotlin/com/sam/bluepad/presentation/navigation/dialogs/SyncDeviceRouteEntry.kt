package com.sam.bluepad.presentation.navigation.dialogs

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sync.SyncConnectorScreen
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncConnectorViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.syncDeviceRouteEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SyncConnectorRoute> {

    val viewModel = koinViewModel<SyncConnectorViewModel>()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    SyncConnectorScreen(
        state = screenState,
        onEvent = viewModel::onEvent,
        navigation = {
            if (backStack.isNotEmpty()) {
                IconButton(onClick = { backStack.removeLastOrNull() }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = stringResource(Res.string.action_back)
                    )
                }
            }
        },
    )
}