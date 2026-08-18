package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sam.bluepad.domain.compression.CompressionLevel

@Composable
fun CompressionLevelSelector(
    onLevelChange: (CompressionLevel) -> Unit,
    modifier: Modifier = Modifier,
    selectedLevel: CompressionLevel = CompressionLevel.LEVEL_3,
    enabled: Boolean = true,
) {

    var showDropDown by remember { mutableStateOf(false) }

    ListItem(
        onClick = {},
        trailingContent = {
            Box {
                Button(
                    onClick = { showDropDown = !showDropDown },
                    shapes = ButtonDefaults.shapes(),
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(
                        "LEVEL_${selectedLevel.level}",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
                DropdownMenu(
                    expanded = showDropDown,
                    onDismissRequest = { showDropDown = false },
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    CompressionLevel.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text("LEVEL ${entry.level}") },
                            onClick = {
                                onLevelChange(entry)
                                showDropDown = false
                            },
                        )
                    }
                }
            }
        },
        supportingContent = { Text(text = "Compression reduces the bandwidth of data being passed") },
        content = {
            Text(
                text = "Payload compression level",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        },
        enabled = enabled,
        modifier = modifier,
    )
}
