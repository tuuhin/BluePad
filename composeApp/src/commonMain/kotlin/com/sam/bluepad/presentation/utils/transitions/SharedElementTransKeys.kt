package com.sam.bluepad.presentation.utils.transitions

import kotlin.uuid.Uuid

object SharedElementTransKeys {
    fun sharedContentSketch(id: Uuid) = "shared-content-sketch-card-container-${id}"
    fun sharedElementSketchTitle(id: Uuid) = "shared-element-sketch-card-content-${id}"

    const val SHARED_BOUNDS_RECEIVE_FROM_OTHER_DEVICE = "shared-bounds-receive-from-other-device-screen"
    const val SHARED_BOUNDS_CREATE_NEW_SKETCH = "shared-bounds-create-new-sketch"
}
