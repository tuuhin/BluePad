package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_copy
import com.sam.bluepad.resources.ic_delete
import com.sam.bluepad.resources.ic_share
import com.sam.bluepad.resources.ic_vert_menu
import com.sam.bluepad.resources.menu_action_copy
import com.sam.bluepad.resources.menu_action_delete
import com.sam.bluepad.resources.menu_action_share
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SketchCard(
	sketch: SketchModel,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onDelete: (() -> Unit)? = null,
	onShare: (() -> Unit)? = null,
	onCopy: (() -> Unit)? = null
) {
	var showContextActions by remember { mutableStateOf(false) }

	Card(
		onClick = onClick,
		shape = MaterialTheme.shapes.extraLarge,
		modifier = modifier
	) {
		Column(
			modifier = Modifier.fillMaxWidth()
				.padding(Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = sketch.title,
					style = MaterialTheme.typography.titleMediumEmphasized,
					color = MaterialTheme.colorScheme.onSurface,
					overflow = TextOverflow.Ellipsis,
					maxLines = 2,
					modifier = Modifier.weight(1f)
				)
				Spacer(modifier = Modifier.width(8.dp))
				Box {
					IconButton(onClick = { showContextActions = true }) {
						Icon(
							painter = painterResource(Res.drawable.ic_vert_menu),
							contentDescription = null,
						)
					}
					DropdownMenu(
						expanded = showContextActions,
						onDismissRequest = { showContextActions = false },
						shape = MaterialTheme.shapes.medium
					) {
						ContextActions.entries.forEach { entry ->
							DropdownMenuItem(
								text = { Text(entry.actionText) },
								leadingIcon = {
									Icon(
										painter = entry.actionIcon,
										contentDescription = null
									)
								},
								enabled = when (entry) {
									ContextActions.COPY -> onCopy != null
									ContextActions.DELETE -> onDelete != null
									ContextActions.SHARE -> onShare != null
								},
								colors = if (entry == ContextActions.DELETE)
									MenuDefaults.itemColors(
										textColor = MaterialTheme.colorScheme.error,
										leadingIconColor = MaterialTheme.colorScheme.error
									)
								else MenuDefaults.itemColors(),
								onClick = {
									when (entry) {
										ContextActions.COPY -> onCopy?.invoke()
										ContextActions.DELETE -> onDelete?.invoke()
										ContextActions.SHARE -> onShare?.invoke()
									}
								},
							)
						}
					}
				}
			}
			HorizontalDivider()
			Text(
				text = sketch.content,
				style = MaterialTheme.typography.bodyMediumEmphasized,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				overflow = TextOverflow.Ellipsis,
				maxLines = 5,
			)
		}
	}
}

private enum class ContextActions {
	COPY,
	SHARE,
	DELETE,
}

private val ContextActions.actionText: String
	@Composable
	get() = when (this) {
		ContextActions.COPY -> stringResource(Res.string.menu_action_copy)
		ContextActions.SHARE -> stringResource(Res.string.menu_action_share)
		ContextActions.DELETE -> stringResource(Res.string.menu_action_delete)
	}

private val ContextActions.actionIcon: Painter
	@Composable
	get() = when (this) {
		ContextActions.COPY -> painterResource(Res.drawable.ic_copy)
		ContextActions.SHARE -> painterResource(Res.drawable.ic_share)
		ContextActions.DELETE -> painterResource(Res.drawable.ic_delete)
	}

@Preview
@Composable
fun SketchCardPreview() = BluePadTheme {
	SketchCard(sketch = PreviewFakes.FAKE_SKETCH_MODEL, onClick = {}, onDelete = {})
}