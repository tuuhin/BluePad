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

    private val nativeTargetName: String
        get() {
            val os = OperatingSystem.current()
            return when {
                os.isWindows -> "mingwX64"
                os.isMacOsX -> {
                    val arch = System.getProperty("os.arch")
                    if (arch == "aarch64" || arch == "arm64") "macosArm64" else "macosX64"
                }

                os.isLinux -> "linuxX64"
                else -> throw GradleException("Invalid desktop target")
            }
        }

    private val libraryExt: String
        get() {
            val os = OperatingSystem.current()
            return when {
                os.isWindows -> "*.dll"
                os.isMacOsX -> "*.dylib"
                os.isLinux -> "*.so"
                else -> throw GradleException("Invalid target")
            }
        }

    private val osSimpleName: String
        get() {
            val os = OperatingSystem.current()
            return when {
                os.isWindows -> "windows"
                os.isMacOsX -> "macos"
                os.isLinux -> "linux"
                else -> throw GradleException("Invalid target")
            }
        }

    override fun apply(target: Project) {
        target.setupCopyAndDeleteTaskForDist()
        target.includePathForRunForTestAndExec()
    }

    private fun Project.includePathForRunForTestAndExec() {
        tasks.withType<Test>().configureEach { setUpProjectPathForRun(this@includePathForRunForTestAndExec) }
        tasks.withType<JavaExec>().configureEach { setUpProjectPathForRun(this@includePathForRunForTestAndExec) }
    }

    private fun Project.setupCopyAndDeleteTaskForDist() {
        val deleteLibs = tasks.register<Delete>("deleteNativeLibraryForPackaging") {
            description = "delete the associated lib copy in desktop resources"

            val targetDir = layout.projectDirectory.dir("desktopResources/$osSimpleName/libs")
            delete(targetDir)
        }

        val copyNativeLib = tasks.register<Copy>("copyNativeLibraryForDist") {
            description = "Copies the native libraries to desktop resources to final distributable"

            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            dependsOn(deleteLibs)

            for (subproject in rootProject.subprojects) {
                subproject.evaluationDependsOn(subproject.path)

                val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>()
                val target = kotlinExp?.targets?.findByName(nativeTargetName) as? KotlinNativeTarget
                    ?: continue

                for (binary in target.binaries) {
                    from(binary.linkTaskProvider.map { it.destinationDirectory }) {
                        include(libraryExt)
                    }
                    val copyTaskName = "copyTo${binary.name.replaceFirstChar(Char::uppercase)}"
                    dependsOn(subproject.tasks.matching { it.name == copyTaskName })
                }
            }
            val targetDir = layout.projectDirectory.dir("desktopResources/$osSimpleName/libs")
            into(targetDir)
        }

        tasks.configureEach {
            if (name == "prepareAppResources" || name.startsWith("package") || name.startsWith("createDistributable")) {
                dependsOn(copyNativeLib)
            }
        }

        tasks.named("clean") {
            mustRunAfter(deleteLibs)
        }
    }

    private fun Task.setUpProjectPathForRun(project: Project) {
        val forkOptions = this as? ProcessForkOptions ?: return

        // Extract subproject output directory Providers safely during configuration phase
        val binaryDirectories = project.rootProject.subprojects.mapNotNull { subproject ->
            val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>() ?: return@mapNotNull null
            kotlinExp.targets
                .filterIsInstance<KotlinNativeTarget>()
                .flatMap { it.binaries }
                .onEach { binary ->
                    this@setUpProjectPathForRun.dependsOn(binary.linkTaskProvider)
                }
                .map { binary -> binary.linkTaskProvider.map { it.destinationDirectory.get().asFile } }
        }.flatten()

        // Defer resolving path until execution phase without referencing `Project`
        doFirst {
            val binDirs = binaryDirectories.mapNotNull { provider ->
                val dir = provider.orNull
                if (dir != null && dir.exists()) dir.absolutePath else null
            }.toSet()
            if (binDirs.isEmpty()) return@doFirst

            val pathSeparator = File.pathSeparator
            val existingPath = System.getenv("PATH") ?: ""

            logger.debug("LIST OF LIBRARIES PATH LINKED :{}", binDirs)

            val mergedPath = (binDirs + existingPath.split(pathSeparator))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(pathSeparator)

            forkOptions.environment("PATH", mergedPath)
        }
    }
}
