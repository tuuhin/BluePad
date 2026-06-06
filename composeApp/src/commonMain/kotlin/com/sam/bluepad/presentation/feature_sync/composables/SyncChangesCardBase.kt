package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_expanded_list
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.compose.resources.painterResource

@Composable
fun SyncChangeBaseCard(
    change: SyncChanges,
    modifier: Modifier = Modifier,
    actions: @Composable (ColumnScope.() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues.Zero,
    entries: ImmutableMap<String, String> = persistentMapOf(),
    extraEntries: ImmutableMap<String, String> = persistentMapOf(),
    entriesTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
    entriesTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    extraEntriesTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    extraEntriesTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    var showDetails by remember { mutableStateOf(false) }

    val transition = updateTransition(showDetails)
    val rotateZ by transition.animateFloat { shown -> if (shown) 0f else 180f }

    Surface(
        onClick = { showDetails = !showDetails },
        shape = shape,
        border = BorderStroke(1.dp, change.secondaryContentColor),
        color = change.primaryBackGround,
        contentColor = change.primaryContentColor,
        modifier = modifier.animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = change.secondaryBackgroundColor,
                    border = BorderStroke(1.dp, color = change.secondaryContentColor),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = change.markerChipText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColorFor(change.secondaryContentColor),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                    )
                }
                IconButton(
                    onClick = { showDetails = !showDetails },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = change.secondaryBackgroundColor,
                        contentColor = change.secondaryContentColor,
                    ),
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_expanded_list),
                        contentDescription = if (showDetails) "Hide details" else "Show details",
                        modifier = Modifier.graphicsLayer {
                            rotationZ = rotateZ
                        },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { (key, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = key.uppercase(),
                            style = entriesTitleStyle,
                            color = change.secondaryContentColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = value,
                            maxLines = if (showDetails) 5 else 2,
                            overflow = TextOverflow.Ellipsis,
                            style = entriesTextStyle,
                            color = change.primaryContentColor,
                        )
                    }
                }
            }
            transition.AnimatedVisibility(
                visible = { it },
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                        .background(color = change.secondaryBackgroundColor, shape = shape)
                        .padding(12.dp),
                ) {
                    extraEntries.forEach { (key, value) ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = key,
                                style = extraEntriesTitleStyle,
                                color = change.secondaryContentColor,
                            )
                            Text(
                                text = value,
                                style = extraEntriesTextStyle,
                                color = change.secondaryContentColor,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            if (actions != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                actions()
            }
        }
    }
}

private val SyncChanges.primaryBackGround: Color
    @Composable
    get() = when (this) {
        is SyncChanges.Conflict -> Color(0x33F5D0FE)
        is SyncChanges.Delete -> Color(0x33FEE2E2)
        is SyncChanges.Insert -> Color(0x33BBF7D0)
        is SyncChanges.Update -> Color(0x3386EFAC)
    }

private val SyncChanges.secondaryBackgroundColor: Color
    @Composable
    get() = when (this) {
        is SyncChanges.Conflict -> Color(0x73D8B4FE)
        is SyncChanges.Delete -> Color(0x73FCA5A5)
        is SyncChanges.Insert -> Color(0x7386EFAC)
        is SyncChanges.Update -> Color(0x73FCD34D)
    }

private val SyncChanges.secondaryContentColor: Color
    @Composable
    get() {
        val isDarkTheme = isSystemInDarkTheme()
        return when (this) {
            is SyncChanges.Conflict -> if (isDarkTheme) Color(0xffe879f9) else Color(0xff581c87)
            is SyncChanges.Delete -> if (isDarkTheme) Color(0xfff87171) else Color(0xffb91c1c)
            is SyncChanges.Insert -> if (isDarkTheme) Color(0xff4ade80) else Color(0xff14532d)
            is SyncChanges.Update -> if (isDarkTheme) Color(0xfffbbf24) else Color(0xff78350f)
        }
    }


private val SyncChanges.primaryContentColor: Color
    @Composable
    get() {
        val isDarkTheme = isSystemInDarkTheme()
        return when (this) {
            is SyncChanges.Conflict -> if (isDarkTheme) Color(0xff9333ea) else Color(0xff3b0764)
            is SyncChanges.Delete -> if (isDarkTheme) Color(0xffdc2626) else Color(0xff7f1d1d)
            is SyncChanges.Insert -> if (isDarkTheme) Color(0xff16a34a) else Color(0xff052e16)
            is SyncChanges.Update -> if (isDarkTheme) Color(0xffd97706) else Color(0xff451a03)
        }
    }


private val SyncChanges.markerChipText: String
    @Composable
    get() = when (this) {
        is SyncChanges.Conflict -> "CONFLICT"
        is SyncChanges.Delete -> "DELETE"
        is SyncChanges.Insert -> "INSERT"
        is SyncChanges.Update -> "UPDATE"
    }
