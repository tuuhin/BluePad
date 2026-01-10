package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.theme.Dimensions

@Composable
fun SketchCard(
	sketch: SketchModel,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		onClick = onClick,
		shape = MaterialTheme.shapes.extraLarge,
		modifier = modifier
	) {
		Column(
			modifier = Modifier.fillMaxWidth()
				.padding(Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Text(
				text = sketch.title,
				style = MaterialTheme.typography.titleMediumEmphasized,
				color = MaterialTheme.colorScheme.onSurface,
				overflow = TextOverflow.Ellipsis,
				maxLines = 2,
			)
			Text(
				text = sketch.content,
				style = MaterialTheme.typography.bodyMediumEmphasized,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				overflow = TextOverflow.Ellipsis,
				maxLines = 4,
			)
		}
	}
}