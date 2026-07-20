package com.sam.bluepad.plugins.ext

import com.sam.bluepad.plugins.extensions.KTNativeJNAExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal fun Project.configureWindowsTasks(
    extension: KTNativeJNAExtension,
    buildDirPath: String = "cmake"
) {
    val cmakeBuildDir = layout.buildDirectory.dir(buildDirPath).get().asFile

    val configure = tasks.register<Exec>("cmakeConfigure") {
        group = "build"
        description = "Configure the CMake to run with the given project"

        doFirst { cmakeBuildDir.mkdirs() }
        workingDir(cmakeBuildDir)

        commandLine(
            "cmd", "/c", "cmake.exe",
            "-G", "Visual Studio 17 2022",
            "-A", "x64",
            "-DCMAKE_CXX_COMPILER=cl.exe",
            "-S", extension.cmakeFilePath.get().asFile.absolutePath,
            "-B", cmakeBuildDir.absolutePath,
        )
    }

    val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
        group = "build"
        description = "Perform CMake build on the configuration"
        dependsOn(configure)
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }

        doFirst {
            val isRelease = extension.releaseBuildEnabled.getOrElse(false)
            commandLine(
                "cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath,
                "--config", if (isRelease) "Release" else "Debug",
            )
        }
    }

    val cmakeClean = tasks.register<Exec>("cmakeClean") {
        group = "clean"
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }
        commandLine("cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath, "--target", "clean")
    }

    tasks.matching { it.name == "clean" }.configureEach { dependsOn(cmakeClean) }

    val copyBleAdvertiseDllToKne = tasks.register<Copy>("copyNativeLibsToKne") {
        group = "kne"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        dependsOn(cmakeBuild)
        mustRunAfter("copyKneNativeLib")

        val isRelease = extension.releaseBuildEnabled.getOrElse(false)
        if (isRelease) from(layout.buildDirectory.dir("cmake/bin/Release"))
        else from(layout.buildDirectory.dir("cmake/bin/Debug"))

        include("*.dll")
        into(layout.buildDirectory.dir("generated/kne/nativeLib/kne/native/win32-x64"))
    }

    val kmpExt = extensions.getByType<KotlinMultiplatformExtension>()
    kmpExt.targets.whenObjectAdded {
        if (this !is KotlinJvmTarget) return@whenObjectAdded
        tasks.named("jvmProcessResources") { dependsOn(copyBleAdvertiseDllToKne) }
    }

    tasks.withType<Test>().configureEach { setupNativePath(this@configureWindowsTasks) }
    tasks.withType<JavaExec>().configureEach { setupNativePath(this@configureWindowsTasks) }
}

