package com.sam.bluepad.presentation.feature_sketches.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_sketches.composables.CreateScreenContent
import com.sam.bluepad.presentation.feature_sketches.composables.CreateScreenTopAppBar
import com.sam.bluepad.presentation.feature_sketches.composables.DeleteSketchDialog
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchScreenEvent
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_save
import com.sam.bluepad.resources.action_update
import com.sam.bluepad.resources.ic_add
import com.sam.bluepad.resources.ic_sketch_update
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSketchScreen(
	state: CreateSketchState,
	onEvent: (CreateSketchScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	isContentLoadFailed: Boolean = false,
	navigation: @Composable () -> Unit = {}
) {

	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current
	val windowSize = LocalWindowSizeInfo.current


	DeleteSketchDialog(
		showDialog = state.showDeleteDialog,
		onCancel = { onEvent(CreateSketchScreenEvent.OnToggleDeleteDialog) },
		onConfirm = { onEvent(CreateSketchScreenEvent.OnConfirmDeleteSketch) },
	)

	Scaffold(
		topBar = {
			CreateScreenTopAppBar(
				navigation = navigation,
				onDelete = { onEvent(CreateSketchScreenEvent.OnToggleDeleteDialog) },
				showDeleteAction = !state.isNewContent,
				showActions = true,
				topBarScrollBehaviour = topBarScrollBehaviour,
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = {
					if (state.isNewContent) onEvent(CreateSketchScreenEvent.OnSaveSketch)
					else onEvent(CreateSketchScreenEvent.OnUpdateSketch)
				},
				text = {
					Text(
						text = if (state.isNewContent)
							stringResource(Res.string.action_save)
						else stringResource(Res.string.action_update)
					)
				},
				icon = {
					Icon(
						painter = if (state.isNewContent)
							painterResource(Res.drawable.ic_add)
						else painterResource(Res.drawable.ic_sketch_update),
						contentDescription = "Save Action"
					)
				},
				shape = MaterialTheme.shapes.extraLarge,
				expanded = windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		ContentLoadingWrapper(
			content = state,
			isLoading = isLoading,
			isContentLoadFailed = isContentLoadFailed,
			modifier = Modifier.fillMaxSize().padding(padding),
			onSuccess = {
				CreateScreenContent(
					state = state,
					contentPadding = PaddingValues(
						horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
						vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
					),
					modifier = Modifier.fillMaxSize()
				)
			},
			onFailed = {},
		)
	}
}