package com.sam.bluepad.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

class KTNucleusPackagingExtPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.setupCopyAndDeleteTaskForDist()
        target.includePathForRunForTestAndExec()
    }

    private fun Project.includePathForRunForTestAndExec() {
        tasks.withType<Test>().configureEach { setUpProjectPathForRun(this@includePathForRunForTestAndExec) }
        tasks.withType<JavaExec>().configureEach { setUpProjectPathForRun(this@includePathForRunForTestAndExec) }
    }

    private fun Project.setupCopyAndDeleteTaskForDist() {

        val copyNativeLib = tasks.register<Copy>("copyNativeLibraryForDist") {
            description = "Copies the native libraries to desktop resources to final distributable"
            duplicatesStrategy = DuplicatesStrategy.INCLUDE

            val os = OperatingSystem.current()

            val nativeTargetName = when {
                os.isWindows -> "mingwX64"
                os.isMacOsX -> {
                    val arch = System.getProperty("os.arch")
                    if (arch == "aarch64" || arch == "arm64") "macosArm64" else "macosX64"
                }

                os.isLinux -> "linuxX64"
                else -> throw GradleException("Invalid desktop target")
            }

            val binDirs = mutableListOf<String>()

            for (subproject in rootProject.subprojects) {
                val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>()
                val target = kotlinExp?.targets?.findByName(nativeTargetName) as? KotlinNativeTarget
                    ?: continue
                target.binaries.forEach { binary -> binDirs.add(binary.outputFile.parentFile.absolutePath) }
            }

            val ext = when {
                os.isWindows -> "*.dll"
                os.isMacOsX -> "*.dylib"
                os.isLinux -> "*.so"
                else -> throw GradleException("Invalid target")
            }

            val path = when {
                os.isWindows -> "windows"
                os.isMacOsX -> "macos"
                os.isLinux -> "linux"
                else -> throw GradleException("Invalid target")
            }

            from(binDirs) {
                include(ext)
            }

            val targetDir = layout.projectDirectory.dir("desktopResources/$path/libs")
            into(targetDir)
        }

        val deleteLibs = tasks.register<Delete>("deleteNativeLibraryForPackaging") {
            description = "delete the associated lib copy in desktop resources"

            val os = OperatingSystem.current()
            val path = when {
                os.isWindows -> "windows"
                os.isMacOsX -> "macos"
                os.isLinux -> "linux"
                else -> throw GradleException("Invalid target")
            }

            val targetDir = layout.projectDirectory.dir("desktopResources/$path/libs")
            delete(targetDir)
        }

        tasks.matching {
            it.name == "prepareAppResources" ||
                it.name.startsWith("package") ||
                it.name.startsWith("createDistributable")
        }.configureEach {
            dependsOn(copyNativeLib)
        }

        tasks.named("clean") {
            mustRunAfter(deleteLibs)
        }
    }

    /**
     * We need to set the paths for the native library as our library has a transitive dependency
     */
    private fun Task.setUpProjectPathForRun(project: Project) {

        val binDirs = mutableListOf<String>()

        // check all the subprojects for kmp ext and take the native targets
        for (subproject in project.rootProject.subprojects) {
            val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>()
            val targets = kotlinExp?.targets ?: continue
            val nativeBinaries = targets.filterIsInstance<KotlinNativeTarget>()
                .flatMap { it.binaries }

            nativeBinaries.forEach { binary ->
                binDirs.add(binary.outputFile.parentFile.absolutePath)
            }
        }

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
}
