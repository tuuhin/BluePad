package com.sam.bluepad.presentation.feature_devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sam.bluepad.presentation.utils.LocalSnackBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDevicesScreen(
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {}
) {
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current

	var isFavExpanded by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }


	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = "Manage Devices") },
				navigationIcon = navigation,
				scrollBehavior = topBarScrollBehaviour
			)
		},
		floatingActionButton = {
			FloatingActionButtonMenu(
				expanded = isFavExpanded,
				button = {
					TooltipBox(
						positionProvider = TooltipDefaults
							.rememberTooltipPositionProvider(positioning = if (isFavExpanded) TooltipAnchorPosition.Below else TooltipAnchorPosition.Below),
						tooltip = { PlainTooltip { Text("Toggle menu") } },
						state = rememberTooltipState(),
					) {
						ToggleFloatingActionButton(
							modifier = Modifier.animateFloatingActionButton(
								visible = isFavExpanded,
								alignment = Alignment.BottomEnd,
							).focusRequester(focusRequester),
							checked = isFavExpanded,
							onCheckedChange = { isFavExpanded = !isFavExpanded },
						) {

						}
					}
				},
			) {

			}
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		Column(
			modifier = Modifier.padding(padding)
		) {

		}
	}
}