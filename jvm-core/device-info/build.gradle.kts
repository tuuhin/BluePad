import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.compression.ext)
}

kotlin {

    jvmToolchain(22)

    val currentOs: OperatingSystem = OperatingSystem.current()
    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main") {
                cinterops.create("deviceInfo") {
                    definitionFile = project.file("src/nativeInterops/cinterops/win32bluetooth.def")
                    packageName = "com.sam.bluepad.windows.bluetooth"
                }
            }
        }

        currentOs.isMacOsX -> macosArm64()
        currentOs.isLinux -> linuxX64()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExport {
    nativeLibName = "aboutDevice"
    nativePackage = "com.sam.bluepad.platform.device_info"

    // env will get precedence over gradle property
    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

    val envTypeIsRelease = envNativeBuildType.getOrElse("false")
        .toBoolean()

    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false")
        .toBoolean()

    buildType = if (envTypeIsRelease || isPropertyTypeRelease) "debug" else "release"
}

ktUpxCompressor {
    enabled = true
}


