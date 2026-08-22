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
                cinterops.create("dpApi") {
                    definitionFile = project.file("src/nativeInterop/cinterop/windows_dpapi.def")
                    packageName = "com.sam.bluepad.windows.dpapi"
                }
            }
        }

        currentOs.isMacOsX -> macosArm64 {
            compilations.getByName("main") {
                cinterops.create("keychain") {
                    definitionFile = project.file("src/nativeInterop/cinterop/macos_keychain.def")
                    packageName = "com.sam.bluepad.osx.keychain"
                }
            }
        }

        currentOs.isLinux -> linuxX64 {
            compilations.getByName("main") {
                cinterops.create("libSecret") {
                    definitionFile.set(project.file("src/nativeInterop/cinterop/linux_libsecret.def"))
                    packageName = "com.sam.bluepad.libsecret"
                    compilerOpts(pkgConfigFlags("--cflags", "libsecret-1"))
                    linkerOpts(pkgConfigFlags("--libs", "libsecret-1"))
                }
            }
        }

        else -> throw GradleException("Platform not supported")
    }

    jvm()

    sourceSets {
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

kotlinNativeExport {
    nativeLibName = "bluepadCrypto"
    nativePackage = "com.sam.bluepad.platform.native"
    // env will get precedence over gradle property
    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

    // Evaluate release mode
    val envTypeIsRelease = envNativeBuildType.getOrElse("false").toBoolean()
    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false").toBoolean()
    val isReleaseBuild = envTypeIsRelease || isPropertyTypeRelease
    buildType = if (isReleaseBuild) "debug" else "release"
}


ktUpxCompressor {
    enabled = true
}

fun pkgConfigFlags(flag: String, library: String): List<String> {
    return try {
        val cFlagsProvider = providers.exec {
            commandLine("pkg-config", flag, library)
        }
        cFlagsProvider.standardOutput.asText.get()
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
