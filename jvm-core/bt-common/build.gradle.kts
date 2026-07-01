import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.nucleus.nna)
}

// env will get precedence over gradle property
val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")

val currentOs: OperatingSystem = OperatingSystem.current()
val generatedPackageName = "com.sam.bt_common.platform"

kotlin {
    jvmToolchain(22)
    val cInterOpName = "btCommon"

    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_bt_common.def"))
                packageName = "$generatedPackageName.mingw"
                includeDirs(rootProject.file("cpp/windows/bt_common/include"))
                compilerOpts("-Wno-c99-designator")
            }

            binaries.all {
                // Linker needs to find .lib files. Search both Debug and Release dirs.
                val libDebugPath = layout.buildDirectory.dir("cmake/lib/Debug").get().asFile.absolutePath
                val libReleasePath = layout.buildDirectory.dir("cmake/lib/Release").get().asFile.absolutePath

                val dllDebugPath = layout.buildDirectory.dir("cmake/bin/Debug")
                val dllReleasePath = layout.buildDirectory.dir("cmake/bin/Release")

                linkerOpts("-L${libReleasePath}", "-L${libDebugPath}", "-lbt_common")

                val taskName = "copyBtCommonDllTo${name.replaceFirstChar(Char::uppercase)}"
                val copyDllToLinkDir = tasks.register<Copy>(taskName) {
                    group = "kne"
                    description = "Copies secondary dll files to shared bin directory"
                    from(dllDebugPath)
                    from(dllReleasePath)
                    include("*.dll")
                    into(linkTaskProvider.flatMap { it.destinationDirectory })
                    dependsOn("cmakeBuild")
                }
                linkTaskProvider.configure { finalizedBy(copyDllToLinkDir) }
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
                compilerOpts(pkgConfigFlags("--cflags", "libbluetooth-dev"))
            }
            binaries.all {
                linkerOpts(pkgConfigFlags("--libs", "libbluetooth-dev"))
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

// Config for Kotlin Native Export / JVM Interop
kotlinNativeExport {
    nativeLibName = "btCommonNative"
    nativePackage = "com.sam.bt_common.platform"

    val envTypeIsRelease = envNativeBuildType.getOrElse("false")
        .toBoolean()

    val isPropertyTypeRelease = propertiesBuildType.getOrElse("false")
        .toBoolean()

    buildType = if (envTypeIsRelease || isPropertyTypeRelease) "debug" else "release"
}

if (currentOs.isWindows) {
    val cmakeBuildDir = layout.buildDirectory.dir("cmake").get().asFile
    val cmakeProjectDir = rootProject.file("cpp/windows/bt_common")


    tasks.register<Exec>("cmakeConfigure") {
        group = "build"
        description = "Configure the cmake to run with project dir and build dir"
        doFirst { cmakeBuildDir.mkdirs() }
        workingDir(cmakeBuildDir)
        commandLine(
            "cmd",
            "/c",
            "cmake.exe",
            "-G",
            "Visual Studio 17 2022",
            "-A",
            "x64",
            "-DCMAKE_CXX_COMPILER=cl.exe",
            "-S",
            cmakeProjectDir.absolutePath,
            "-B",
            cmakeBuildDir.absolutePath,
        )
    }

    tasks.register<Exec>("cmakeBuild") {
        group = "build"
        description = "Perform Cmake build on the configuration"
        dependsOn("cmakeConfigure")
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }

        val envTypeIsRelease = envNativeBuildType.getOrElse("false")
            .toBoolean()

        val isPropertyTypeRelease = propertiesBuildType.getOrElse("false")
            .toBoolean()

        val cmakeBuildType = if (envTypeIsRelease || isPropertyTypeRelease) "Release" else "Debug"
        commandLine("cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath, "--config", cmakeBuildType)
    }

    tasks.register<Exec>("cmakeClean") {
        group = "clean"
        description = "Cleans the cmake files"
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }
        commandLine("cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath, "--target", "clean")
    }

    tasks.named("cinteropBtCommonMingwX64") { dependsOn("cmakeBuild") }
    tasks.named("clean") { dependsOn("cmakeClean") }

    val copyBtCommonDllToKne = tasks.register<Copy>("copyBtCommonDllToKne") {
        group = "kne"
        description = "Copies bt_common.dll to the kne resource directory for bundling"
        from(layout.buildDirectory.dir("cmake/bin/Release"))
        from(layout.buildDirectory.dir("cmake/bin/Debug"))
        include("bt_common.dll")
        into(layout.buildDirectory.dir("generated/kne/nativeLib/kne/native/win32-x64"))
        dependsOn("cmakeBuild")
        mustRunAfter("copyKneNativeLib")
    }

    tasks.named("jvmProcessResources") { dependsOn(copyBtCommonDllToKne) }

    tasks.withType<Test>().configureEach { setupNativePath() }
    tasks.withType<JavaExec>().configureEach { setupNativePath() }
}

// Helper to inject native binary dirs into PATH for JVM runtime/tests
fun Task.setupNativePath() {
    if (!currentOs.isWindows) return
    val mingwTarget = kotlin.targets.findByName("mingwX64") as? KotlinNativeTarget ?: return
    val binDirs = mingwTarget.binaries.map { it.outputFile.parentFile.absolutePath }.distinct()
    val existingPath = System.getenv("PATH") ?: ""
    val newPath = (binDirs + existingPath.split(";")).distinct().joinToString(";")

    if (this is ProcessForkOptions) environment("PATH", newPath)
}

fun pkgConfigFlags(flag: String, library: String): List<String> {
    return try {
        providers.exec { commandLine("pkg-config", flag, library) }
            .standardOutput.asText.get().trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    } catch (_: Exception) {
        emptyList()
    }
}
