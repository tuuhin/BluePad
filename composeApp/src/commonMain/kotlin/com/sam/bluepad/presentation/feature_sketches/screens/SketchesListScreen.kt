package com.sam.bluepad.presentation.feature_sketches.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.presentation.feature_sketches.composables.EmptySketchesList
import com.sam.bluepad.presentation.feature_sketches.composables.SketchesListContent
import com.sam.bluepad.presentation.feature_sketches.events.SketchScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_create_sketch
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.resources.sketches_screen_title
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchesListScreen(
	sketches: ImmutableList<SketchModel>,
	onEvent: (SketchScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	navigation: @Composable () -> Unit = {},
	onNavigateToSketch: (SketchModel) -> Unit = {},
	onNavigateToNewSketch: () -> Unit = {},
) {
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current
	val windowSize = LocalWindowSizeInfo.current

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.sketches_screen_title)) },
				navigationIcon = navigation,
				scrollBehavior = topBarScrollBehaviour
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = onNavigateToNewSketch,
				text = { Text(text = stringResource(Res.string.action_create_sketch)) },
				icon = {
					Icon(
						painter = painterResource(Res.drawable.ic_add),
						contentDescription = "Add"
					)
				},
				expanded = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		ListContentLoadingWrapper(
			content = sketches,
			isLoading = isLoading,
			modifier = Modifier.padding(padding),
			onEmpty = {
				EmptySketchesList(
					onCreateNew = onNavigateToNewSketch,
					modifier = Modifier.fillMaxSize()
				)
			},
			onItems = { sketches ->
				SketchesListContent(
					sketches = sketches,
					onSelectSketch = onNavigateToSketch,
					onDeleteSketch = { sketch -> onEvent(SketchScreenEvent.OnDeleteSketch(sketch)) },
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(
						horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
						vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
					)
				)
			},
		)
	}
}