package com.sam.bluepad.plugins

import com.sam.bluepad.plugins.ext.catalog
import com.sam.bluepad.plugins.ext.configureLinuxTask
import com.sam.bluepad.plugins.ext.configureMacOsTask
import com.sam.bluepad.plugins.ext.configureWindowsTasks
import com.sam.bluepad.plugins.extensions.CmakeOsBuild
import com.sam.bluepad.plugins.extensions.KTNativeJNAExtension
import dev.nucleusframework.nna.plugin.KotlinNativeExportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KTNativeJNAPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyPlugins()

        val customExt = project.createExtension()
        project.setUpCustomNNA(customExt)
        project.configureKotlinMultiplatform()
    }

    private fun Project.configureKotlinMultiplatform() {
        val kmpExt = extensions.getByType<KotlinMultiplatformExtension>()
        val nnaExt = extensions.getByType<KTNativeJNAExtension>()

        val currentOs = OperatingSystem.current()
        val buildOptions = nnaExt.cmakeBuildOptions.get()

        val isOsSupported = when {
            currentOs.isWindows -> CmakeOsBuild.WINDOWS in buildOptions
            currentOs.isMacOsX -> CmakeOsBuild.MACOS in buildOptions
            currentOs.isLinux -> CmakeOsBuild.LINUX in buildOptions
            else -> false
        }

        if (!isOsSupported) return

        // Use withType lazy container iteration instead of direct eager callbacks
        kmpExt.targets.withType<KotlinNativeTarget>().configureEach {
            configureLinkLibraryTask(this)
        }
    }

    private fun Project.applyPlugins() {
        val aliases = listOf("kotlinMultiplatform", "nucleus-nna")
        aliases.forEach {
            catalog.findPlugin(it).ifPresent { libraryProvider ->
                val plugin = libraryProvider.get()
                plugins.apply(plugin.pluginId)
            }
        }
    }

    private fun Project.setUpCustomNNA(ktNativeJnaExt: KTNativeJNAExtension) {
        extensions.getByType<KotlinNativeExportExtension>().apply {
            nativePackage.set(ktNativeJnaExt.generatedPackageName)
            nativeLibName.set(ktNativeJnaExt.nativeLibName)
            buildType.set(ktNativeJnaExt.releaseBuildEnabled.map { if (it) "release" else "debug" })
        }

        val currentOs = OperatingSystem.current()
        val buildOptions = ktNativeJnaExt.cmakeBuildOptions.get()

        when {
            currentOs.isWindows && CmakeOsBuild.WINDOWS in buildOptions -> configureWindowsTasks(ktNativeJnaExt)
            currentOs.isMacOsX && CmakeOsBuild.MACOS in buildOptions -> configureMacOsTask(ktNativeJnaExt)
            currentOs.isLinux && CmakeOsBuild.LINUX in buildOptions -> configureLinuxTask(ktNativeJnaExt)
        }
    }

    private fun Project.createExtension(): KTNativeJNAExtension {
        val extension = extensions.create<KTNativeJNAExtension>("kotlinNativeExportCmakeExt")
        extension.apply {
            generatedPackageName.convention("com.sam.bluepad.platform")
            releaseBuildEnabled.convention(false)
            cmakeBuildOptions.convention(listOf(CmakeOsBuild.WINDOWS))
        }
        return extension
    }

    private fun Project.configureLinkLibraryTask(nativeTarget: KotlinNativeTarget) {
        val os = OperatingSystem.current()
        val filePattern = when {
            os.isWindows -> "*.dll"
            os.isMacOsX -> "*.dylib"
            os.isLinux -> "*.a"
            else -> return
        }

        nativeTarget.binaries.configureEach {
            val buildDir = layout.buildDirectory

            // Safely retrieve provider directory paths without premature file realization during configuration
            val libDebugPath = buildDir.dir("cmake/lib/Debug").map { it.asFile.absolutePath }
            val libReleasePath = buildDir.dir("cmake/lib/Release").map { it.asFile.absolutePath }

            // Dynamic mapping ensures lazily evaluated linker flags
            linkerOpts("-L${libReleasePath.get()}", "-L${libDebugPath.get()}")

            // Register copy task safely
            val taskName = "copyTo${name.replaceFirstChar(Char::uppercase)}"
            val copyNativeToPath = tasks.register<Copy>(taskName) {
                group = "kne"
                description = "Copies secondary library files to shared bin directory"
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                from(buildDir.dir("cmake/bin/Debug"))
                from(buildDir.dir("cmake/bin/Release"))
                include(filePattern)
                into(linkTaskProvider.flatMap { it.destinationDirectory })

                // Lazy dependency resolution to avoid eager task realization
                dependsOn(tasks.matching { it.name == "cmakeBuild" })
            }

            linkTaskProvider.configure {
                dependsOn(tasks.matching { it.name == "cmakeBuild" })
                finalizedBy(copyNativeToPath)
            }
        }
    }
}
