package com.sam.bluepad.plugins.ext

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

internal fun Task.setupNativePath(target: Project) {

    val kotlin = target.extensions.getByType<KotlinMultiplatformExtension>()

    val nativeTargets = kotlin.targets.filterIsInstance<KotlinNativeTarget>()
    if (nativeTargets.isEmpty()) return

    val binDirs = nativeTargets.flatMap { target ->
        target.binaries.map { it.outputFile.parentFile.absolutePath }
    }.distinct().filterNotNull()

    if (binDirs.isEmpty()) return

    val pathSeparator = File.pathSeparator

    val existingPath = System.getenv("PATH") ?: ""
    val mergedPath = (binDirs + existingPath.split(pathSeparator))
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(pathSeparator)

    if (this !is ProcessForkOptions) return
    environment("PATH", mergedPath)

}
