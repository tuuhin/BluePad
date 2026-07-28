package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.settings.models.AppFontOption
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_paint_brush
import com.sam.bluepad.resources.ic_typeface
import com.sam.bluepad.resources.settings_personalization_use_device_font_text
import com.sam.bluepad.resources.settings_personalization_use_device_font_title
import com.sam.bluepad.resources.settings_personalization_use_dynamic_color_text
import com.sam.bluepad.resources.settings_personalization_use_dynamic_color_title
import com.sam.bluepad.resources.settings_segment_personalization
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreenContent(
    state: SettingsScreenState,
    onEvent: (SettingsScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        if (state.device != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CurrentDeviceInfoCard(
                    deviceState = state.device,
                    devicePlatform = state.platformOs,
                    onUpdateName = { onEvent(SettingsScreenEvent.OnUpdateDeviceName(it)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(Res.string.settings_segment_personalization),
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item {
            ListItem(
                checked = state.appSettings.fontOption == AppFontOption.SYSTEM,
                onCheckedChange = { onEvent(SettingsScreenEvent.OnToggleAppFont) },
                trailingContent = {
                    Switch(
                        checked = state.appSettings.fontOption == AppFontOption.SYSTEM,
                        onCheckedChange = null,
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_typeface),
                        contentDescription = null,
                    )
                },
                supportingContent = { Text(text = stringResource(Res.string.settings_personalization_use_device_font_text)) },
                content = {
                    Text(
                        text = stringResource(Res.string.settings_personalization_use_device_font_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                },
            )
        }
        item {
            ListItem(
                checked = state.appSettings.useDynamicColor,
                onCheckedChange = { onEvent(SettingsScreenEvent.UseDynamicColor) },
                trailingContent = {
                    Switch(
                        checked = state.appSettings.useDynamicColor,
                        onCheckedChange = null,
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_paint_brush),
                        contentDescription = null,
                    )
                },
                supportingContent = { Text(text = stringResource(Res.string.settings_personalization_use_dynamic_color_text)) },
                content = {
                    Text(
                        text = stringResource(Res.string.settings_personalization_use_dynamic_color_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                },
            )
        }
    }
}

