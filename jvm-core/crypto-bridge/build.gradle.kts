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
        "mingwX64" -> mingwX64 {
            compilations.getByName("main") {
                cinterops.create("dpApi") {
                    definitionFile = project.file("src/nativeInterop/cinterop/windows_dpapi.def")
                    packageName = "com.sam.bluepad.windows.dpapi"
                }
            }
        }

        "macosArm64" -> macosArm64 {
            compilations.getByName("main") {
                cinterops.create("keychain") {
                    definitionFile = project.file("src/nativeInterop/cinterop/macos_keychain.def")
                    packageName = "com.sam.bluepad.osx.keychain"
                }
            }
        }

        "linuxX64" -> linuxX64 {
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
        val process = ProcessBuilder("pkg-config", flag, library).start()
        val result = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0 && result.isNotEmpty()) {
            result.split(" ")
        } else emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
