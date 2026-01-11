package com.sam.bluepad.data.interactions

import com.sam.bluepad.domain.exceptions.ShareActionNotAvailableException
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import com.sam.bluepad.domain.models.SketchModel

actual class ShareSketchInteractionImpl : ShareSketchInteraction {

	override fun shareSketch(sketch: SketchModel): Result<Unit> {
		return Result.failure(ShareActionNotAvailableException())
	}
}