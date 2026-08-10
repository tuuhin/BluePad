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

internal fun Project.configureLinuxTask(
    extension: KTNativeJNAExtension,
    buildDirPath: String = "cmake"
) {
    val cmakeBuildDir = layout.buildDirectory.dir(buildDirPath).get().asFile

    val configure = tasks.register<Exec>("cmakeConfigure") {
        group = "build"
        description = "Configure CMake for Linux"

        doFirst { cmakeBuildDir.mkdirs() }
        workingDir(cmakeBuildDir)

        val isRelease = extension.releaseBuildEnabled.getOrElse(false)
        val buildType = if (isRelease) "Release" else "Debug"

        commandLine(
            "cmake",
            "-DCMAKE_BUILD_TYPE=$buildType",
            "-S", extension.cmakeFilePath.get().asFile.absolutePath,
            "-B", cmakeBuildDir.absolutePath,
        )
    }

    val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
        group = "build"
        description = "Perform CMake build on Linux configuration"
        dependsOn(configure)
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }

        val isRelease = extension.releaseBuildEnabled.getOrElse(false)
        val buildType = if (isRelease) "Release" else "Debug"

        commandLine(
            "cmake",
            "--build", cmakeBuildDir.absolutePath,
            "-DCMAKE_BUILD_TYPE=$buildType",
        )
    }

    val cmakeClean = tasks.register<Exec>("cmakeClean") {
        group = "clean"
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }
        commandLine("cmake", "--build", cmakeBuildDir.absolutePath, "--target", "clean")
    }

    tasks.matching { it.name == "clean" }.configureEach {
        dependsOn(cmakeClean)
    }

    val copyNativeLibsToKne = tasks.register<Copy>("copyNativeLibsToKne") {
        group = "kne"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        dependsOn(cmakeBuild)
        mustRunAfter("copyKneNativeLib")

        val sourceDirProvider = layout.buildDirectory.dir(
            if (extension.releaseBuildEnabled.getOrElse(false)) "cmake/bin/Release" else "cmake/bin/Debug",
        )

        from(sourceDirProvider) {
            include("*.so", "*.a")
        }

        into(layout.buildDirectory.dir("generated/kne/nativeLib/kne/native/darwin"))
    }

    val kmpExt = extensions.getByType<KotlinMultiplatformExtension>()
    kmpExt.targets.matching { it is KotlinJvmTarget }.configureEach {
        tasks.named("jvmProcessResources") {
            dependsOn(copyNativeLibsToKne)
        }
    }

    tasks.withType<Test>().configureEach { setupNativePath(this@configureLinuxTask) }
    tasks.withType<JavaExec>().configureEach { setupNativePath(this@configureLinuxTask) }
}

