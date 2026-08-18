package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.sam.bluepad.domain.settings.models.SyncSettingsModel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

@Composable
fun PayloadSizeSelector(
    onUpdatePayloadSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialValue: Int = SyncSettingsModel.MIN_SYNC_CHUNK_SIZE,
    enabled: Boolean = true,
) {

    var sliderValue by remember { mutableFloatStateOf(initialValue.toFloat()) }
    val valueRange = remember {
        SyncSettingsModel.MIN_SYNC_CHUNK_SIZE.toFloat()..SyncSettingsModel.MAX_SYNC_CHUNK_SIZE.toFloat()
    }

    val sliderState = rememberSliderState(
        value = initialValue.toFloat(),
        valueRange = valueRange,
        onValueChangeFinished = { onUpdatePayloadSize(sliderValue.roundToInt()) },
    )

    LaunchedEffect(sliderState) {
        snapshotFlow { sliderState.value }
            .buffer(2)
            .onEach { sliderValue = it }
            .launchIn(this)
    }

    val sliderValueAsInt by remember(sliderState) {
        derivedStateOf { sliderState.value.fastRoundToInt() }
    }


    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = "Set payload size",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = "Payload size determine speed and error rate for sync")
                Slider(
                    state = sliderState,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        trailingContent = {
            Text(
                text = "$sliderValueAsInt",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
    )
}
