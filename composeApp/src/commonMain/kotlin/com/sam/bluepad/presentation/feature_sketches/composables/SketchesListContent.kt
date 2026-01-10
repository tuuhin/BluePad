package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.SketchModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SketchesListContent(
	sketches: ImmutableList<SketchModel>,
	onSelectSketch: (SketchModel) -> Unit,
	onDeleteSketch: (SketchModel) -> Unit,
	modifier: Modifier = Modifier,
	listState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
	contentPadding: PaddingValues = PaddingValues.Zero,
) {

	val isLocalInspectionMode = LocalInspectionMode.current

	val listKey: ((Int, SketchModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.id.toHexString() }
	}

	val contentType: ((Int, SketchModel) -> Any?) = remember {
		{ _, _ -> SketchModel::class.simpleName }
	}

	LazyVerticalStaggeredGrid(
		state = listState,
		columns = StaggeredGridCells.Adaptive(minSize = 200.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalItemSpacing = 8.dp,
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(
			items = sketches,
			key = listKey,
			contentType = contentType
		) { _, sketch ->
			SketchCard(
				sketch = sketch,
				onClick = { onSelectSketch(sketch) },
				modifier = Modifier.animateItem()
			)
		}
	}
}