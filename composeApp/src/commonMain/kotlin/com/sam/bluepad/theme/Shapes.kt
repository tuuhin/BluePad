package com.sam.bluepad.theme

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

internal class MorphOverlayClip(
    val morph: Morph,
    val progress: () -> Float
) : SharedTransitionScope.OverlayClip {

    private val matrix = Matrix()
    private val composePath = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density
    ): Path {
        matrix.reset()
        composePath.reset()

        val max = maxOf(bounds.width, bounds.height)
        morph.toPath(progress(), path = composePath)

        matrix.scale(max, max)
        composePath.transform(matrix)

        val translationOffset = bounds.center + Offset(-max * 0.5f, -max * 0.5f)
        composePath.translate(translationOffset)
        return composePath
    }

}
