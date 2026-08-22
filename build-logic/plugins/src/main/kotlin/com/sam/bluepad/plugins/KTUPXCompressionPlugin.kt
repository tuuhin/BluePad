package com.sam.bluepad.plugins

import com.sam.bluepad.plugins.extensions.KTUPXExtension
import com.sam.bluepad.plugins.extensions.UpxCompressionLevel
import com.sam.bluepad.plugins.extensions.UpxStrategy
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary

class KTUPXCompressionPlugin : Plugin<Project> {

    private val pluginName = "ktUpxCompressor"

    override fun apply(project: Project) {
        val extension = project.createExtension()
        project.configureKotlinMultiplatform(extension)
    }

    private fun Project.createExtension(): KTUPXExtension {
        return extensions.create<KTUPXExtension>(pluginName).apply {
            enabled.convention(false)
            strategy.convention(UpxStrategy.DEFAULT)
            level.convention(UpxCompressionLevel.BALANCED)
        }
    }

    private fun Project.configureKotlinMultiplatform(upxExt: KTUPXExtension) {
        val kmpExt = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        kmpExt.targets.withType<KotlinNativeTarget>().configureEach {
            configureUpxCompressionTask(this, upxExt)
        }
    }

    private fun Project.configureUpxCompressionTask(
        nativeTarget: KotlinNativeTarget,
        upxExt: KTUPXExtension
    ) {
        val os = OperatingSystem.current()
        val extensionFilter = when {
            os.isWindows -> "dll"
            os.isMacOsX -> "dylib"
            os.isLinux -> "so"
            else -> return
        }

        // Filter specifically for SharedLib binaries to avoid null compilation metadata errors
        nativeTarget.binaries.withType<SharedLibrary>().configureEach {
            // Cross-platform linker stripping options applied globally
            val sharedLib = this
            val linkTask = sharedLib.linkTaskProvider

            when {
                os.isWindows -> linkerOpts("-s")
                os.isLinux -> linkerOpts("-s", "-Wl,--gc-sections", "-Wl,--strip-all")
                os.isMacOsX -> linkerOpts("-Wl,-dead_strip")
            }

            // Compiler optimizations applied globally
            freeCompilerArgs += listOf(
                "-Xbackend-threads=0",
                "-Xbinary=smallBinary=true",      // Reduces runtime size safely
                "-Xbinary=sourceInfoType=noop",   // Drops debug line info tables
                "-Xbinary=latin1Strings=true",    // Compresses 16-bit string constants
                "-Xbinary=bundleId=",              // Drops extra bundle metadata
            )

            val taskName = "compressWithUpxFor${name.replaceFirstChar(Char::uppercase)}"

            val compressTask = tasks.register<Exec>(taskName) {
                group = "build"
                description = "Compresses compiled Kotlin Native target binary using UPX"

                dependsOn(linkTask)
                onlyIf {
                    // Execute UPX ONLY if plugin is enabled AND binary is a RELEASE build
                    upxExt.enabled.get() && sharedLib.buildType == NativeBuildType.RELEASE
                }

                doFirst {
                    val binaryFile = sharedLib.outputFile

                    if (!binaryFile.exists() || binaryFile.extension != extensionFilter) {
                        logger.warn("[UPX Plugin] Binary file not found or extension mismatch at: ${binaryFile.absolutePath}")
                        return@doFirst
                    }

                    val cliArgs = mutableListOf<String>().apply {
                        add(upxExt.level.get().flag)
                        upxExt.strategy.get().flag?.let { flag -> add(flag) }
                        add(binaryFile.absolutePath)
                    }

                    commandLine("upx", *cliArgs.toTypedArray())
                    logger.lifecycle("[UPX Plugin] Compressing binary ${binaryFile.name} using options: $cliArgs")
                }

                isIgnoreExitValue = true
            }

            linkTask.configure {
                finalizedBy(compressTask)
            }
        }
    }
}
