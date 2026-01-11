package com.sam.bluepad.data.interactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import com.sam.bluepad.domain.interactions.CopySketchInteraction
import com.sam.bluepad.domain.models.SketchModel

actual class CopySketchInteractionImpl(private val context: Context) : CopySketchInteraction {

	private val _clipboard by lazy { context.getSystemService<ClipboardManager>() }

	override suspend fun copyToClipboard(sketch: SketchModel): Result<Boolean> {
		return try {
			val clipData = ClipData.newPlainText("SKetch", sketch.content)
			_clipboard?.setPrimaryClip(clipData)
			Result.success(false)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}