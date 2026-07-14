package com.sam.bluepad.plugins

import com.sam.bluepad.plugins.ext.catalog
import com.sam.bluepad.plugins.ext.configureWindowsTasks
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

        kmpExt.targets.whenObjectAdded {
            if (this !is KotlinNativeTarget) return@whenObjectAdded
            val os = OperatingSystem.current()
            when {
                os.isWindows -> configureWinNativeTask(this)
            }
        }
    }

    private fun Project.configureWinNativeTask(nativeTarget: KotlinNativeTarget) {
        nativeTarget.binaries.all {
            val buildDir = layout.buildDirectory
            val libDebugPath = buildDir.dir("cmake/lib/Debug").get().asFile.absolutePath
            val libReleasePath = buildDir.dir("cmake/lib/Release").get().asFile.absolutePath

            linkerOpts("-L$libReleasePath", "-L$libDebugPath")

            val taskName = "copyTo${name.replaceFirstChar(Char::uppercase)}"
            val copyDllToLinkDir = tasks.register<Copy>(taskName) {
                group = "kne"
                description = "Copies secondary dll files to shared bin directory"
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                from(buildDir.dir("cmake/bin/Debug"))
                from(buildDir.dir("cmake/bin/Release"))
                include("*.dll")
                into(linkTaskProvider.flatMap { it.destinationDirectory })
                dependsOn("cmakeBuild")
            }
            linkTaskProvider.configure { finalizedBy(copyDllToLinkDir) }
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
        val nnaExt = extensions.getByType<KotlinNativeExportExtension>()
        nnaExt.nativePackage.set(ktNativeJnaExt.generatedPackageName)
        nnaExt.nativeLibName.set(ktNativeJnaExt.nativeLibName)

        nnaExt.buildType.set(ktNativeJnaExt.releaseBuildEnabled.map { isRelease -> if (isRelease) "release" else "debug" })

        val currentOs = OperatingSystem.current()
        when {
            currentOs.isWindows -> configureWindowsTasks(ktNativeJnaExt)
        }
    }

    private fun Project.createExtension(): KTNativeJNAExtension {

        val extension = extensions.create<KTNativeJNAExtension>("kotlinNativeExportCmakeExt")
        extension.apply {
            generatedPackageName.convention("com.sam.bluepad.platform")
            releaseBuildEnabled.convention(false)
        }
        return extension
    }
}
