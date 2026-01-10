package com.sam.bluepad.presentation.feature_sketches.events

import androidx.compose.foundation.text.input.TextFieldState

data class CreateSketchState(
	val contentTitleState: TextFieldState = TextFieldState(),
	val contentTextState: TextFieldState = TextFieldState(),
	val isNewContent: Boolean = false,
	val showDeleteDialog: Boolean = false,
)