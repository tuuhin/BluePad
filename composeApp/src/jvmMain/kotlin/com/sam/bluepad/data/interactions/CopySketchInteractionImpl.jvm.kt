package com.sam.bluepad.data.interactions

import com.sam.bluepad.domain.interactions.CopySketchInteraction
import com.sam.bluepad.domain.models.SketchModel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual class CopySketchInteractionImpl : CopySketchInteraction {

	private val _clipboard by lazy { Toolkit.getDefaultToolkit().systemClipboard }

	override suspend fun copyToClipboard(sketch: SketchModel): Result<Boolean> {
		return try {
			val selection = StringSelection(sketch.content)
			_clipboard.setContents(selection, null)
			Result.success(true)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}