package com.sam.bluepad.domain.interactions

import com.sam.bluepad.domain.models.SketchModel

fun interface ShareSketchInteraction {

	fun shareSketch(sketch: SketchModel): Result<Unit>
}