import com.sam.bluepad.plugins.extensions.CmakeOsBuild
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.nna.cmake.ext)
    alias(libs.plugins.nucleus.compression.ext)
}

group = "com.sam.bluepad"

kotlin {

    jvmToolchain(22)

    val os: OperatingSystem = OperatingSystem.current()
    when {
        os.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create("shareSheet") {
                definitionFile = project.file("src/nativeInterop/cinterops/windows_share_sheet.def")
                packageName = "com.sam.bluepad.shareSheet.mingw"
                includeDirs(rootProject.file("cpp/windows/share_sheet/include"))
                binaries.all {
                    linkerOpts("-lshare_sheet")
                }
            }
        }

        os.isMacOsX -> macosArm64()
        os.isLinux -> linuxX64()
        else -> throw GradleException("Invalid Desktop target")
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.datetime)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExportCmakeExt {

    nativeLibName = "ktShareSheet"
    generatedPackageName = "com.sam.bluepad.native.shareSheet"
    cmakeFilePath = rootProject.file("cpp/windows/share_sheet")
    cmakeBuildOptions = listOf(CmakeOsBuild.WINDOWS)

    // env will get precedence over gradle property
    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")
    val isRelease = envNativeBuildType.getOrElse("false").toBoolean()
        || propertiesBuildType.getOrElse("false").toBoolean()

    releaseBuildEnabled = isRelease
}

ktUpxCompressor {
    enabled = true
}
