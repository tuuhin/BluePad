import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nucleus.nna)
}

val currentOs: OperatingSystem = OperatingSystem.current()

// env will get precedence over gradle property
val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

kotlin {

    jvmToolchain(22)

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
    val envTypeIsRelease = envNativeBuildType.getOrElse("false")
        .toBoolean()

    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false")
        .toBoolean()

    buildType = if (envTypeIsRelease || isPropertyTypeRelease) "debug" else "release"
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
