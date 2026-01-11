package com.sam.bluepad.data.interactions

import android.content.Context
import android.content.Intent
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import com.sam.bluepad.domain.models.SketchModel

actual class ShareSketchInteractionImpl(private val context: Context) : ShareSketchInteraction {

	override fun shareSketch(sketch: SketchModel): Result<Unit> {
		return try {
			val intent = Intent(Intent.ACTION_SEND).apply {
				type = "text/plain"
				putExtra(Intent.EXTRA_SUBJECT, sketch.title)
				putExtra(Intent.EXTRA_TEXT, sketch.content)
			}

			val intentChooser = Intent
				.createChooser(intent, "Select application to share to").apply {
					flags = Intent.FLAG_ACTIVITY_NEW_TASK
				}
			context.startActivity(intentChooser)
			Result.success(Unit)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}