package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AppKit.NSView
import platform.Foundation.NSValue
import platform.CoreGraphics.CGPointMake
import platform.QuartzCore.CASpringAnimation
import platform.QuartzCore.CATransform3DIdentity
import platform.QuartzCore.CATransform3DMakeScale
import platform.QuartzCore.kCAFillModeForwards
import platform.QuartzCore.valueWithCATransform3D

internal fun NSView.playPopIn(durationMs: Int = 200) {
    wantsLayer = true
    val layer = layer ?: return

    layer.anchorPoint = CGPointMake(0.5, 0.5)
    val size = layer.frame.useContents { size }
    layer.position = CGPointMake(size.width / 2.0, size.height / 2.0)

    val startTransform = CATransform3DMakeScale(0.85, 0.85, 1.0)

    layer.transform = startTransform

    val animation = CASpringAnimation().apply {
        keyPath = "transform"
        fromValue = NSValue.valueWithCATransform3D(startTransform)
        toValue = NSValue.valueWithCATransform3D(CATransform3DIdentity.readValue())
        damping = 14.0
        stiffness = 180.0
        mass = 1.0
        initialVelocity = 0.0

        duration = durationMs / 1000.0
        fillMode = kCAFillModeForwards
        removedOnCompletion = false
    }

    layer.addAnimation(animation, forKey = "toast.popIn")
    layer.transform = CATransform3DIdentity.readValue()
}

internal fun NSView.playPopOut(durationMs: Int = 120) {
    wantsLayer = true
    val layer = layer ?: return

    layer.anchorPoint = CGPointMake(0.5, 0.5)
    val size = layer.frame.useContents { size }
    layer.position = CGPointMake(size.width / 2.0, size.height / 2.0)

    val end = CATransform3DMakeScale(0.85, 0.85, 1.0)

    val animation = CASpringAnimation().apply {
        keyPath = "transform"
        fromValue = NSValue.valueWithCATransform3D(CATransform3DIdentity.readValue())
        toValue = NSValue.valueWithCATransform3D(end)

        damping = 18.0
        stiffness = 240.0
        mass = 1.0
        initialVelocity = 0.0

        duration = durationMs / 1000.0
        fillMode = kCAFillModeForwards
        removedOnCompletion = false
    }

    layer.addAnimation(animation, forKey = "toast.popOut")
    layer.transform = end
}


fun NSView.removeToastAnimations() {
    val layer = layer ?: return
    layer.removeAnimationForKey("toast.popIn")
    layer.removeAnimationForKey("toast.popOut")
}
