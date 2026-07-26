plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
}

val hostOs: String = System.getProperty("os.name")
val hostTarget = when {
    hostOs == "Linux" -> "linuxX64"
    hostOs == "Mac OS X" -> "macosArm64"
    hostOs.startsWith("Windows") -> "mingwX64"
    else -> error("Unsupported host OS: $hostOs")
}

// env will get precedence over gradle property
val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

kotlin {

    jvmToolchain(22)

    when (hostTarget) {
        "mingwX64" -> mingwX64()
        "macosArm64" -> macosArm64()
        "linuxX64" -> linuxX64()
    }

    jvm()

    sourceSets {
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
    val envTypeIsRelease = envNativeBuildType.getOrElse("false")
        .toBoolean()

    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false")
        .toBoolean()

    buildType = if (envTypeIsRelease || isPropertyTypeRelease) "debug" else "release"
}
