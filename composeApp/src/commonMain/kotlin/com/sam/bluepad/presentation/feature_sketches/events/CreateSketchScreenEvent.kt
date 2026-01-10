package com.sam.bluepad.presentation.feature_sketches.events

sealed interface CreateSketchScreenEvent {
	data object OnSaveSketch : CreateSketchScreenEvent
	data object OnUpdateSketch : CreateSketchScreenEvent
	data object OnDeleteSketch : CreateSketchScreenEvent
}