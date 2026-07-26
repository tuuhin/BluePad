package com.sam.bluepad.presentation.navigation.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.presentation.core.AppCommonViewModel
import com.sam.bluepad.presentation.composables.RequestBTEnableDialog
import com.sam.bluepad.presentation.navigation.nav_graph.RootTabLayoutNavGraph
import com.sam.bluepad.presentation.utils.LocalAnimatedContentScope
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.presentation.utils.transitions.sharedTransitionRenderInOverlay
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.bluetooth_not_enabled_text
import com.sam.bluepad.resources.ic_menu
import com.sam.bluepad.resources.ic_menu_close
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNavigationSuitWrapper(
    modifier: Modifier = Modifier,
    initialRailExpanded: Boolean = false,
    selectedRoute: RootTabLayoutNavGraph? = RootTabLayoutNavGraph.ListRoute,
    onSelectRoute: (RootTabLayoutNavGraph) -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val windowSize = LocalWindowSizeInfo.current
    val contentScope = LocalAnimatedContentScope.current

    val snackBarHostState = remember { SnackbarHostState() }

    val storeOwner = rememberViewModelStoreOwner()
    val viewModel = koinViewModel<AppCommonViewModel>(viewModelStoreOwner = storeOwner)

    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()

    UiEventsHandler(
        eventsFlow = viewModel::uiEvent,
        snackBarState = snackBarHostState,
    )

    var showDialog by rememberSaveable { mutableStateOf(false) }

    RequestBTEnableDialog(
        showDialog = showDialog,
        onDismiss = { showDialog = false },
        canOpenSettings = bluetoothState.canOpenBTSettings,
        canRequestActivate = bluetoothState.canRequestBTActive,
        onOpenSettings = {
            viewModel.onOpenAppSettings()
            showDialog = false
        },
        onActivate = {
            viewModel.onRequestEnableBT()
            showDialog = false
        },
    )

    val blocks = remember {
        persistentListOf(
            RootTabLayoutNavGraph.ListRoute,
            RootTabLayoutNavGraph.DeviceRoute,
            RootTabLayoutNavGraph.SettingsRoute,
        )
    }
    Scaffold(
        bottomBar = {
            AppNavigationBar(
                items = blocks,
                onSelectRoute = onSelectRoute,
                selectedRoute = selectedRoute ?: RootTabLayoutNavGraph.ListRoute,
                showNavBar = !windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
                modifier = Modifier.sharedTransitionRenderInOverlay(1f)
                    .then(
                        if (contentScope == null)
                            Modifier else with(contentScope) {
                            Modifier.animateEnterExit(
                                enter = slideInVertically { height -> height } + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = slideOutVertically { height -> height } + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            )
                        },
                    ),
            )
        },
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { scPadding ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
                .padding(scPadding),
        ) {
            AppNavigationRail(
                items = blocks,
                onSelectRoute = onSelectRoute,
                showNavRail = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
                selectedRoute = selectedRoute ?: RootTabLayoutNavGraph.ListRoute,
                initialRailExpanded = initialRailExpanded,
                modifier = Modifier.sharedTransitionRenderInOverlay(1f)
                    .then(
                        if (contentScope == null)
                            Modifier else with(contentScope) {
                            Modifier.animateEnterExit(
                                enter = slideInHorizontally { width -> -width } + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = slideOutHorizontally { width -> -width } + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            )
                        },
                    ),
            )
            ContainerContent(
                isBtActive = bluetoothState.isBTActive,
                content = content,
                onRequestBTActive = { showDialog = true },
                modifier = Modifier.weight(1f)
                    .fillMaxHeight()
                    .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
            )
        }
    }
}


@Composable
private fun AppNavigationBar(
    items: ImmutableList<RootTabLayoutNavGraph>,
    onSelectRoute: (RootTabLayoutNavGraph) -> Unit,
    modifier: Modifier = Modifier,
    showNavBar: Boolean = false,
    selectedRoute: RootTabLayoutNavGraph = RootTabLayoutNavGraph.ListRoute,
    containerColor: Color = NavigationBarDefaults.containerColor,
    elevation: Dp = NavigationBarDefaults.Elevation
) {
    AnimatedVisibility(
        visible = showNavBar,
        enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { height -> height },
        exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { height -> height },
        modifier = modifier,
    ) {
        NavigationBar(
            containerColor = containerColor,
            tonalElevation = elevation,
            contentColor = contentColorFor(containerColor),
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = if (item == selectedRoute) item.routeFilledIcon
                            else item.routeOutlinedIcon,
                            contentDescription = item.routeName,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(item.routeName) },
                    selected = selectedRoute == item,
                    onClick = { onSelectRoute(item) },
                )
            }
        }
    }
}

@Composable
private fun AppNavigationRail(
    items: ImmutableList<RootTabLayoutNavGraph>,
    onSelectRoute: (RootTabLayoutNavGraph) -> Unit,
    modifier: Modifier = Modifier,
    showNavRail: Boolean = false,
    selectedRoute: RootTabLayoutNavGraph = RootTabLayoutNavGraph.ListRoute,
    initialRailExpanded: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val scope = rememberCoroutineScope()

    val state = rememberWideNavigationRailState(
        initialValue = if (initialRailExpanded) WideNavigationRailValue.Expanded
        else WideNavigationRailValue.Collapsed,
    )

    AnimatedVisibility(
        visible = showNavRail,
        enter = slideInHorizontally(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) { width -> -width },
        exit = slideOutHorizontally(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) { width -> -width },
        modifier = modifier,
    ) {
        WideNavigationRail(
            state = state,
            colors = WideNavigationRailDefaults.colors(
                containerColor = containerColor,
                contentColor = contentColorFor(containerColor),
            ),
            arrangement = if (state.currentValue == WideNavigationRailValue.Expanded)
                Arrangement.spacedBy(4.dp)
            else WideNavigationRailDefaults.arrangement,
            header = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 24.dp),
                ) {
                    IconButton(
                        onClick = { scope.launch { state.toggle() } },
                    ) {
                        if (state.targetValue == WideNavigationRailValue.Expanded) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_menu_close),
                                contentDescription = "Menu close",
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_menu),
                                contentDescription = "menu open",
                            )
                        }
                    }
                }
            },
        ) {
            items.forEach { item ->
                WideNavigationRailItem(
                    railExpanded = state.currentValue == WideNavigationRailValue.Expanded,
                    iconPosition = if (state.currentValue == WideNavigationRailValue.Expanded)
                        NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                    label = { Text(item.routeName) },
                    icon = {
                        Icon(
                            painter = if (item == selectedRoute) item.routeFilledIcon else item.routeOutlinedIcon,
                            contentDescription = item.routeName,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    selected = selectedRoute == item,
                    onClick = { onSelectRoute(item) },
                )
            }
        }
    }
}

@Composable
private fun ContainerContent(
    isBtActive: Boolean,
    onRequestBTActive: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        AnimatedVisibility(
            visible = !isBtActive,
            enter = slideInVertically(MaterialTheme.motionScheme.fastEffectsSpec()) { height -> height }
                + scaleIn(MaterialTheme.motionScheme.slowEffectsSpec(), initialScale = .3f),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 120))
                + scaleOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            modifier = Modifier.align(Alignment.BottomCenter)
                .offset(y = (-10).dp),
        ) {
            SuggestionChip(
                onClick = onRequestBTActive,
                shape = MaterialTheme.shapes.extraLarge,
                label = { Text(text = stringResource(Res.string.bluetooth_not_enabled_text)) },
                border = null,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
        }
    }
}
