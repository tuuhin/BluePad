package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_create_sketch
import com.sam.bluepad.resources.ic_empty_box
import com.sam.bluepad.resources.sketches_list_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptySketchesList(
	onCreateNew: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(Res.drawable.ic_empty_box),
			contentDescription = "No item present",
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
		)
		Spacer(modifier = Modifier.height(12.dp))
		Text(
			text = stringResource(Res.string.sketches_list_empty),
			style = MaterialTheme.typography.titleMediumEmphasized,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(24.dp))
		ElevatedButton(
			onClick = onCreateNew,
			modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary
			),
			shapes = ButtonDefaults.shapes(
				shape = ButtonDefaults.elevatedShape,
				pressedShape = ButtonDefaults.mediumPressedShape
			),
		) {
			Text(text = stringResource(Res.string.action_create_sketch))
		}
	}
}