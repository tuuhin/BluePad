package com.sam.bluepad.presentation.navigation.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.sam.bluepad.presentation.navigation.nav_graph.RootTabLayoutNavGraph
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

val RootTabLayoutNavGraph.routeName: String
    @Composable
    get() = when (this) {
        RootTabLayoutNavGraph.DeviceRoute -> stringResource(Res.string.navigation_device)
        RootTabLayoutNavGraph.ListRoute -> stringResource(Res.string.navigation_list)
        RootTabLayoutNavGraph.SettingsRoute -> stringResource(Res.string.navigation_settings)
    }

val RootTabLayoutNavGraph.routeFilledIcon: Painter
    @Composable
    get() = when (this) {
        RootTabLayoutNavGraph.DeviceRoute -> painterResource(Res.drawable.ic_device_filled)
        RootTabLayoutNavGraph.ListRoute -> painterResource(Res.drawable.ic_note_filled)
        RootTabLayoutNavGraph.SettingsRoute -> painterResource(Res.drawable.ic_settings_filled)
    }

val RootTabLayoutNavGraph.routeOutlinedIcon: Painter
    @Composable
    get() = when (this) {
        RootTabLayoutNavGraph.DeviceRoute -> painterResource(Res.drawable.ic_device_outlined)
        RootTabLayoutNavGraph.ListRoute -> painterResource(Res.drawable.ic_note_outlined)
        RootTabLayoutNavGraph.SettingsRoute -> painterResource(Res.drawable.ic_settings_outlined)
    }
