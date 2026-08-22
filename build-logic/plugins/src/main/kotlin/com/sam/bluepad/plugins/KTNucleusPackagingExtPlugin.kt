package com.sam.bluepad.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
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
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
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
            description = "Deletes the associated lib copy in desktop resources"

            val targetDir = layout.projectDirectory.dir("desktopResources/$osSimpleName/libs")
            delete(targetDir)
        }

        val copyNativeLib = tasks.register<Copy>("copyNativeLibraryForDist") {
            description = "Copies the native libraries to desktop resources to final distributable"

            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            dependsOn(deleteLibs)

            val isReleaseTask = gradle.startParameter.taskNames.any {
                it.contains("release", ignoreCase = true)
            }
            val targetBuildType = if (isReleaseTask) NativeBuildType.RELEASE else NativeBuildType.DEBUG

            rootProject.subprojects {
                val subproject = this
                subproject.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                    val kotlinExp =
                        subproject.extensions.findByType<KotlinMultiplatformExtension>() ?: return@withPlugin

                    kotlinExp.targets.matching { it.name == nativeTargetName }.withType<KotlinNativeTarget>()
                        .configureEach {
                            binaries.withType<SharedLibrary>().matching { it.buildType == targetBuildType }
                                .configureEach {
                                    val sharedLib = this
                                    val linkTask = sharedLib.linkTaskProvider

                                    // Explicitly register dependency and pull directory from provider to fix implicit dependency warnings
                                    dependsOn(linkTask)

                                    // Kotlin/Native also registers a companion "copyTo<BinaryName>" task for each
                                    // SharedLibrary binary (visible on mingwX64, where it stages the .dll alongside
                                    // its .lib/.def files). That task, not linkTask, is what actually writes into
                                    // destinationDirectory on those targets, so it needs its own explicit dependency
                                    // or Gradle flags an implicit-dependency validation warning.
                                    val copyToTaskName =
                                        "copyTo${sharedLib.name.replaceFirstChar { it.uppercaseChar() }}"
                                    dependsOn(subproject.tasks.matching { it.name == copyToTaskName })

                                    from(linkTask.flatMap { it.destinationDirectory }) {
                                        include(libraryExt)
                                    }
                                }
                        }
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

        tasks.matching { it.name == "clean" }.configureEach {
            mustRunAfter(deleteLibs)
        }
    }

    private fun Task.setUpProjectPathForRun(project: Project) {
        val forkOptions = this as? ProcessForkOptions ?: return

        val binaryDirectories = mutableListOf<Pair<NativeBuildType, Provider<File>>>()

        project.rootProject.subprojects {
            val subproject = this
            subproject.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>() ?: return@withPlugin

                kotlinExp.targets.withType<KotlinNativeTarget>().configureEach {
                    binaries.withType<SharedLibrary>().configureEach {
                        val sharedLib = this
                        val linkTask = sharedLib.linkTaskProvider

                        this@setUpProjectPathForRun.dependsOn(linkTask)

                        val dirProvider = linkTask.map { it.destinationDirectory.get().asFile }
                        binaryDirectories.add(sharedLib.buildType to dirProvider)
                    }
                }
            }
        }

        doFirst {
            val isReleaseMode = name.contains("release", ignoreCase = true) ||
                project.gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

            val targetBuildType = if (isReleaseMode) NativeBuildType.RELEASE else NativeBuildType.DEBUG

            val binDirs = binaryDirectories
                .filter { (buildType, _) -> buildType == targetBuildType }
                .mapNotNull { (_, provider) ->
                    val dir = provider.orNull
                    if (dir != null && dir.exists()) dir.absolutePath else null
                }.toSet()

            if (binDirs.isEmpty()) return@doFirst

            val pathSeparator = File.pathSeparator
            val existingPath = System.getenv("PATH") ?: ""

            logger.debug("LIST OF {} LIBRARIES PATH LINKED: {}", targetBuildType, binDirs)

            val mergedPath = (binDirs + existingPath.split(pathSeparator))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(pathSeparator)

            forkOptions.environment("PATH", mergedPath)
        }
    }
}
