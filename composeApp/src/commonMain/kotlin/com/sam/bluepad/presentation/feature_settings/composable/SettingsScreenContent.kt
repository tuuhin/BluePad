package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_settings.event.CurrentDeviceState

@Composable
fun SettingsScreenContent(
    state: CurrentDeviceState,
    onUpdateName: (String) -> Unit,
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
        item(span = { GridItemSpan(maxLineSpan) }) {
            CurrentDeviceInfoCard(
                deviceState = state,
                onUpdateName = onUpdateName,
                modifier = Modifier.animateItem()
            )
        }
    }
}