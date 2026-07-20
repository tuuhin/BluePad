package com.sam.bluepad.presentation.feature_sketches.screens

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_sketches.composables.CreateScreenContent
import com.sam.bluepad.presentation.feature_sketches.composables.CreateScreenTopAppBar
import com.sam.bluepad.presentation.feature_sketches.composables.DeleteSketchDialog
import com.sam.bluepad.presentation.feature_sketches.composables.SaveOrUpdateSketchButton
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchScreenEvent
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.transitions.SharedElementTransKeys
import com.sam.bluepad.presentation.utils.transitions.sharedBoundsWrapper
import com.sam.bluepad.theme.Dimensions
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSketchScreen(
    sketchId: Uuid?,
    state: CreateSketchState,
    onEvent: (CreateSketchScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isContentLoadFailed: Boolean = false,
    navigation: @Composable () -> Unit = {}
) {

    val snackBarHostState = LocalSnackBarState.current
    val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
            SaveOrUpdateSketchButton(
                onUpdateSketch = { onEvent(CreateSketchScreenEvent.OnUpdateSketch) },
                onSaveSketch = { onEvent(CreateSketchScreenEvent.OnSaveSketch) },
                isNewContent = state.isNewContent,
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
            .then(
                if (sketchId == null) Modifier
                    .sharedBoundsWrapper(
                        key = SharedElementTransKeys.SHARED_BOUNDS_CREATE_NEW_SKETCH,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        placeHolderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                        clipShape = MaterialTheme.shapes.extraLarge,
                    )
                else Modifier.sharedBoundsWrapper(
                    key = SharedElementTransKeys.sharedContentSketch(sketchId),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    placeHolderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                    clipShape = MaterialTheme.shapes.extraLarge,
                ),
            ),
    ) { padding ->
        ContentLoadingWrapper(
            content = state,
            isLoading = isLoading,
            isContentLoadFailed = isContentLoadFailed,
            modifier = Modifier.fillMaxSize().padding(padding),
            onLoading = {},
            onSuccess = {
                CreateScreenContent(
                    state = state,
                    sketchId = sketchId,
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                        vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onFailed = {},
        )
    }
}
