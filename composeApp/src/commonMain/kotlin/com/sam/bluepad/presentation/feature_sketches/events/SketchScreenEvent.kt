package com.sam.bluepad.presentation.feature_sketches.events

import com.sam.bluepad.domain.models.SketchModel

sealed interface SketchScreenEvent {
	data class OnSelectSketchToDelete(val sketch: SketchModel) : SketchScreenEvent
	data object OnUnselectSketchToDelete : SketchScreenEvent
	data object OnDeleteSketchConfirm : SketchScreenEvent

	data class OnShareSketch(val sketch: SketchModel) : SketchScreenEvent
	data class OnCopySketch(val sketch: SketchModel) : SketchScreenEvent
}