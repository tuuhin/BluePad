import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.nna.cmake.ext)
}

val cInterOpName = "btCommon"
val generatedPackageName = "com.sam.bt_common.platform"

// env will get precedence over gradle property
val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

kotlin {
    jvmToolchain(22)

    val currentOs: OperatingSystem = OperatingSystem.current()

    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_bt_common.def"))
                packageName = "$generatedPackageName.mingw"
                includeDirs(rootProject.file("cpp/windows/bt_common/include"))
                binaries.all {
                    linkerOpts("-lbt_common")
                }
            }
        }

        currentOs.isMacOsX -> macosArm64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/macosx_bt_common.def"))
                packageName = "$generatedPackageName.osx"
            }
        }

        currentOs.isLinux -> linuxX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/linux_bt_common.def"))
                packageName = "$generatedPackageName.linux"
            }
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
        jvmTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.turbine)
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
    nativeLibName.set("btCommonNative")
    generatedPackageName.set("com.sam.bt_common.platform")
    cInteropName.set("btCommon")
    cmakeFilePath.set(rootProject.file("cpp/windows/bt_common"))

    val isRelease = envNativeBuildType.getOrElse("false").toBoolean()
        || propertiesBuildType.getOrElse("false").toBoolean()
    releaseBuildEnabled.set(isRelease)
}
