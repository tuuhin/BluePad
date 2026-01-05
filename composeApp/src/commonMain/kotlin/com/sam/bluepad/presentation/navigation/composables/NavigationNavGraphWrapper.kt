package com.sam.bluepad.presentation.navigation.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.resources.ic_menu
import com.sam.bluepad.resources.ic_menu_close
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationNavGraphWrapper(
	modifier: Modifier = Modifier,
	initialRailExpanded: Boolean = false,
	selectedRoute: AssociatedNavGraph? = AssociatedNavGraph.ListRoute,
	onSelectRoute: (AssociatedNavGraph) -> Unit = {},
	onNavigateToAddRoute: () -> Unit = {},
	content: @Composable () -> Unit = {},
) {
	val windowSize = LocalWindowSizeInfo.current

	val state = rememberWideNavigationRailState(
		initialValue = if (initialRailExpanded) WideNavigationRailValue.Expanded
		else WideNavigationRailValue.Collapsed
	)

	val scope = rememberCoroutineScope()

	val blocks = remember {
		listOf(
			AssociatedNavGraph.ListRoute,
			AssociatedNavGraph.DeviceRoute,
			AssociatedNavGraph.SettingsRoute,
		)
	}

	Scaffold(
		bottomBar = {
			if (!windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
				NavigationBar {
					blocks.forEach { item ->
						NavigationBarItem(
							icon = {
								Icon(
									painter = if (item == selectedRoute) item.routeFilledIcon else item.routeOutlinedIcon,
									contentDescription = item.routeName,
									modifier = Modifier.size(24.dp)
								)
							},
							label = { Text(item.routeName) },
							selected = selectedRoute == item,
							onClick = { onSelectRoute(item) },
						)
					}
				}
			}
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = { onNavigateToAddRoute() },
				text = { Text(text = "New") },
				icon = {
					Icon(
						painter = painterResource(Res.drawable.ic_add),
						contentDescription = "Add"
					)
				},
				expanded = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
			)
		},
		modifier = modifier,
	) { scPadding ->
		Box(
			modifier = Modifier.fillMaxSize()
				.padding(scPadding)
		) {
			if (windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
				Row(
					modifier = modifier,
					horizontalArrangement = Arrangement.spacedBy(2.dp)
				) {
					WideNavigationRail(
						state = state,
						colors = WideNavigationRailDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
						arrangement = if (state.currentValue == WideNavigationRailValue.Expanded)
							Arrangement.spacedBy(4.dp)
						else WideNavigationRailDefaults.arrangement,
						modifier = modifier,
						header = {
							Column(
								verticalArrangement = Arrangement.spacedBy(4.dp),
								modifier = Modifier.padding(start = 24.dp)
							) {

								IconButton(
									onClick = {
										scope.launch { state.toggle() }
									},
								) {
									if (state.targetValue == WideNavigationRailValue.Expanded) {
										Icon(
											painter = painterResource(Res.drawable.ic_menu_close),
											contentDescription = "Menu close"
										)
									} else {
										Icon(
											painter = painterResource(Res.drawable.ic_menu),
											contentDescription = "menu open"
										)
									}
								}
							}
						},
					) {
						blocks.forEach { item ->
							WideNavigationRailItem(
								railExpanded = state.currentValue == WideNavigationRailValue.Expanded,
								iconPosition = if (state.currentValue == WideNavigationRailValue.Expanded)
									NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
								label = { Text(item.routeName) },
								icon = {
									Icon(
										painter = if (item == selectedRoute) item.routeFilledIcon else item.routeOutlinedIcon,
										contentDescription = item.routeName,
										modifier = Modifier.size(24.dp)
									)
								},
								selected = selectedRoute == item,
								onClick = { onSelectRoute(item) },
							)
						}
					}
					Box(modifier = Modifier.weight(1f)) {
						content()
					}
				}
			} else {
				content()
			}
		}
	}
}