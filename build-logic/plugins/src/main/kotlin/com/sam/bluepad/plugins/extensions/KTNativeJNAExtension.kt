package com.sam.bluepad.plugins.extensions

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface KTNativeJNAExtension {
    val generatedPackageName: Property<String>
    val nativeLibName: Property<String>
    val releaseBuildEnabled: Property<Boolean>
    val cmakeFilePath: DirectoryProperty
}
