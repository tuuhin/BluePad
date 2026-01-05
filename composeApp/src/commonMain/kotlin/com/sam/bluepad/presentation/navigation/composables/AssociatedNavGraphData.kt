package com.sam.bluepad.presentation.navigation.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_device_filled
import com.sam.bluepad.resources.ic_device_outlined
import com.sam.bluepad.resources.ic_note_filled
import com.sam.bluepad.resources.ic_note_outlined
import com.sam.bluepad.resources.ic_settings_filled
import com.sam.bluepad.resources.ic_settings_outlined
import com.sam.bluepad.resources.navigation_device
import com.sam.bluepad.resources.navigation_list
import com.sam.bluepad.resources.navigation_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

val AssociatedNavGraph.routeName: String
	@Composable
	get() = when (this) {
		AssociatedNavGraph.DeviceRoute -> stringResource(Res.string.navigation_device)
		AssociatedNavGraph.ListRoute -> stringResource(Res.string.navigation_list)
		AssociatedNavGraph.SettingsRoute -> stringResource(Res.string.navigation_settings)
	}

val AssociatedNavGraph.routeFilledIcon: Painter
	@Composable
	get() = when (this) {
		AssociatedNavGraph.DeviceRoute -> painterResource(Res.drawable.ic_device_filled)
		AssociatedNavGraph.ListRoute -> painterResource(Res.drawable.ic_note_filled)
		AssociatedNavGraph.SettingsRoute -> painterResource(Res.drawable.ic_settings_filled)
	}

val AssociatedNavGraph.routeOutlinedIcon: Painter
	@Composable
	get() = when (this) {
		AssociatedNavGraph.DeviceRoute -> painterResource(Res.drawable.ic_device_outlined)
		AssociatedNavGraph.ListRoute -> painterResource(Res.drawable.ic_note_outlined)
		AssociatedNavGraph.SettingsRoute -> painterResource(Res.drawable.ic_settings_outlined)
	}