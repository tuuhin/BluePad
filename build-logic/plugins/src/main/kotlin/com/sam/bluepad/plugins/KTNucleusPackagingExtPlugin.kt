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
                    // Explicitly depend on the copy task if it exists (for Windows)
                    val copyTaskName = "copyTo${binary.name.replaceFirstChar(Char::uppercase)}"
                    subproject.tasks.findByName(copyTaskName)?.let {
                        dependsOn(it)
                    }
                }
            }
            val targetDir = layout.projectDirectory.dir("desktopResources/$osSimpleName/libs")
            into(targetDir)
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

        val forkOptions = this as? ProcessForkOptions ?: return

        val binDirs = mutableSetOf<String>()

        // check all the subprojects for kmp ext and take the native targets
        for (subproject in project.rootProject.subprojects) {
            val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>() ?: continue
            val nativeBinaries = kotlinExp.targets.filterIsInstance<KotlinNativeTarget>()
                .flatMap { it.binaries }

            for (binary in nativeBinaries) {
                val file = binary.outputFile
                // Check if it's a dynamic/shared library
                val isLibFile = file.extension in arrayOf("dll", "so", "dylib")
                if (!isLibFile) continue
                binDirs.add(file.parentFile.absolutePath)
                // wait for the library to get build
                dependsOn(binary.linkTaskProvider)
            }
        }

        if (binDirs.isEmpty()) return

        doFirst {
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
