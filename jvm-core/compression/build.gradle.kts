import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.nna.cmake.ext)
}

val currentOs: OperatingSystem = OperatingSystem.current()

group = "com.sam.bluepad.native_compression"

kotlin {

    jvmToolchain(22)

    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create("kompression") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_compression.def"))
                packageName = "com.sam.bluepad.compression.native.mingw"
                includeDirs(rootProject.file("cpp/windows/compression/include"))
                binaries.all {
                    linkerOpts("-lcompression")
                }
            }
        }

        currentOs.isMacOsX -> macosArm64{
            compilations.getByName("main").cinterops.create("kompression") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/macos_compression.def"))
                packageName = "com.sam.bluepad.compression.native.macos"
            }
        }
        currentOs.isLinux -> linuxX64()
    }

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation("at.yawk.lz4:lz4-java:1.11.2")
            implementation("com.github.luben:zstd-jni:1.5.7-12")
        }
        jvmTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExportCmakeExt {
    nativeLibName.set("kompressionNative")
    generatedPackageName.set("com.sam.bluepad.compression.native")
    cmakeFilePath.set(rootProject.file("cpp/windows/compression"))

    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")
    val isRelease = envNativeBuildType.getOrElse("false").toBoolean()
        || propertiesBuildType.getOrElse("false").toBoolean()

    releaseBuildEnabled.set(isRelease)
}
