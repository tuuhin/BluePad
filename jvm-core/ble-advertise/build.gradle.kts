import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.nna.cmake.ext)
}

group = "com.sam.ble_advertise"

// env will get precedence over gradle property
val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

val cInterOpName = "bleAdvertise"
val generatedClassPackageName = "com.sam.ble_advertise.platform"

kotlin {
    jvmToolchain(22)

    val currentOs: OperatingSystem = OperatingSystem.current()
    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_ble_advertise.def"))
                packageName = "$generatedClassPackageName.mingw"
                includeDirs(rootProject.file("cpp/windows/advertise/include"))
                binaries.all {
                    linkerOpts("-lble_advertise")
                }
            }
        }

        currentOs.isMacOsX -> macosArm64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/macos_ble_advertise.def"))
                packageName = "$generatedClassPackageName.osx"
            }
        }

        currentOs.isLinux -> linuxX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/linux_ble_advertise.def"))
                packageName = "$generatedClassPackageName.linux"
            }
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.datetime)
        }
        jvmTest.dependencies {
            implementation(libs.assertk)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExportCmakeExt {
    nativeLibName.set("blePlatformAdvertise")
    generatedPackageName.set(generatedClassPackageName)
    cmakeFilePath.set(rootProject.file("cpp/windows/advertise"))

    val isRelease = envNativeBuildType.getOrElse("false").toBoolean()
        || propertiesBuildType.getOrElse("false").toBoolean()

    releaseBuildEnabled.set(isRelease)
}
