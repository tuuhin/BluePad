package com.sam.bluepad.data.interactions

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.exceptions.ShareActionNotAvailableException
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.native.shareSheet.NativeShareSheetImpl
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

private const val TAG = "ShareSketchInteraction"

@OptIn(ExperimentalAtomicApi::class)
actual class ShareSketchInteractionImpl : ShareSketchInteraction {

    private val _windowHandle = AtomicLong(-1L)

    override fun shareSketch(sketch: SketchModel): Result<Unit> {
        val windowHandle = _windowHandle.load()
        if (windowHandle <= 0) return Result.failure(ShareActionNotAvailableException())

        Logger.d(tag = TAG) { "SHOWING SHARE SHEET" }

        return runCatching {
            NativeShareSheetImpl().use { provider ->
                // otherwise show title and content share sheet
                provider.shareTitleAndContent(
                    windowHandle = windowHandle,
                    title = sketch.title,
                    content = sketch.content,
                )
            }
        }
    }

    override fun setWindowHandle(long: Long) {
        _windowHandle.update { long }
        Logger.d(tag = TAG) { "SET WINDOW HANDLE AS :$long" }
    }
}
