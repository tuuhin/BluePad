plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
}

kotlin {

    jvmToolchain(22)

    val os = org.gradle.internal.os.OperatingSystem.current()
    when {
        os.isWindows -> mingwX64 {
            compilations.getByName("main") {
                cinterops.create("uiAnimations") {
                    definitionFile = project.file("src/nativeInterop/cinterops/window_uianimations.def")
                    packageName = "com.sam.bluepad.common_utils"
                }
                cinterops.create("toastState") {
                    definitionFile = project.file("src/nativeInterop/cinterops/toast_state.def")
                    packageName = "com.sam.bluepad.common_utils"
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
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExport {
    nativeLibName = "commonUtility"
    nativePackage = "com.sam.bluepad.platform.common_utils"
    // env will get precedence over gradle property
    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")
    val envTypeIsRelease = envNativeBuildType.getOrElse("false").toBoolean()
    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false").toBoolean()

    buildType = if (envTypeIsRelease || isPropertyTypeRelease) "debug" else "release"
}
