package com.sam.bluepad.plugins.extensions

import org.gradle.api.provider.Property

interface KTUPXExtension {
    val enabled: Property<Boolean>
    val level: Property<UpxCompressionLevel>
    val strategy: Property<UpxStrategy>
}
