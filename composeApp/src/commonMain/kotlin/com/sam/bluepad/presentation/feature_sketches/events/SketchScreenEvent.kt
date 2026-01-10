package com.sam.bluepad.presentation.feature_sketches.events

import com.sam.bluepad.domain.models.SketchModel

sealed interface SketchScreenEvent {
	data class OnDeleteSketch(val sketch: SketchModel) : SketchScreenEvent
}