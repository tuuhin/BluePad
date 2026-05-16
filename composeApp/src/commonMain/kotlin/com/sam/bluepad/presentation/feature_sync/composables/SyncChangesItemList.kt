package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_sync.state.SyncChangeUIState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SyncChangesItemList(
    items: ImmutableList<SyncChangeUIState>,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues.Zero,
) {
    LazyColumn(
        modifier = modifier, contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { }
}
