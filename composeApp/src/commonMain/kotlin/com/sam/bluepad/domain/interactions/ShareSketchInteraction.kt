package com.sam.bluepad.domain.interactions

import com.sam.bluepad.domain.models.SketchModel

interface ShareSketchInteraction {

    fun setWindowHandle(long: Long)

    fun shareSketch(sketch: SketchModel): Result<Unit>
}
