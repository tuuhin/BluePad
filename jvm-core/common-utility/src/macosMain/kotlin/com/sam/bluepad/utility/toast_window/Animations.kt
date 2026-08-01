package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.readValue
import platform.AppKit.NSView
import platform.Foundation.NSValue
import platform.QuartzCore.*

internal fun NSView.playPopIn(fadeInMs: Int? = 0) {
    wantsLayer = true
    val layer = layer ?: return

    layer.transform = CATransform3DMakeScale(0.85, 0.85, 1.0)

    val scaleAnim = CASpringAnimation().apply {
        keyPath = "transform"
        fromValue = NSValue.valueWithCATransform3D(CATransform3DMakeScale(0.85, 0.85, 1.0))
        toValue = NSValue.valueWithCATransform3D(CATransform3DIdentity.readValue())
        damping = 14.0
        stiffness = 180.0
        mass = 1.0
        initialVelocity = 0.0
        duration = fadeInMs?.toDouble() ?: settlingDuration
        fillMode = kCAFillModeForwards
        removedOnCompletion = false
    }

    layer.addAnimation(scaleAnim, forKey = "popIn")
    layer.transform = CATransform3DIdentity.readValue()
}
