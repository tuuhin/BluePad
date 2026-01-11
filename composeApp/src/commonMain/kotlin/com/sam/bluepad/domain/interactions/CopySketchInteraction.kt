package com.sam.bluepad.domain.interactions

import com.sam.bluepad.domain.models.SketchModel

fun interface CopySketchInteraction {

	/**
	 * Copies the [sketch] content to the clipboard
	 * @return [Result] boolean indicate to show user about the interaction
	 */
	suspend fun copyToClipboard(sketch: SketchModel): Result<Boolean>
}