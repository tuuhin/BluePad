import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.nucleus.nna)
}

group = "com.sam.ble_advertise"

val currentOs: OperatingSystem = OperatingSystem.current()
val nativeExportType = "debug"
val generatedClassPackageName = "com.sam.ble_advertise.platform"

kotlin {
    jvmToolchain(22)
    val cInterOpName = "bleAdvertise"

    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_ble_advertise.def"))
                packageName = "$generatedClassPackageName.mingw"
                includeDirs(rootProject.file("cpp/windows/advertise/include"))
            }

            binaries.all {
                // Linker needs to find .lib files. Search both Debug and Release dirs.
                val libDebugPath = layout.buildDirectory.dir("cmake/lib/Debug").get().asFile.absolutePath
                val libReleasePath = layout.buildDirectory.dir("cmake/lib/Release").get().asFile.absolutePath

                val dllDebugPath = layout.buildDirectory.dir("cmake/bin/Debug")
                val dllReleasePath = layout.buildDirectory.dir("cmake/bin/Release")

                linkerOpts(
                    "-L${libReleasePath}",
                    "-L${libDebugPath}",
                    "-lble_advertise",
                )

                val taskName = "copyBleAdvertiseDllTo${name.replaceFirstChar(Char::uppercase)}"
                val copyDllToLinkDir = tasks.register<Copy>(taskName) {
                    group = "kne"
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
                definitionFile.set(project.file("src/nativeInterop/cinterop/macos_ble_advertise.def"))
                packageName = "$generatedClassPackageName.osx"
            }
        }

        currentOs.isLinux -> linuxX64 {
            compilations.getByName("main").cinterops.create(cInterOpName) {
                definitionFile.set(project.file("src/nativeInterop/cinterop/linux_ble_advertise.def"))
                packageName = "$generatedClassPackageName.linux"
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

// Config for Kotlin Native Export / JVM Interop
kotlinNativeExport {
    nativeLibName = "blePlatformAdvertise"
    nativePackage = generatedClassPackageName
    buildType = nativeExportType
}

if (currentOs.isWindows) {
    val cmakeBuildDir = layout.buildDirectory.dir("cmake").get().asFile
    val cmakeProjectDir = rootProject.file("cpp/windows/advertise")
    val cmakeBuildType = nativeExportType.replaceFirstChar(Char::uppercase)

    tasks.register<Exec>("cmakeConfigure") {
        group = "build"
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
        dependsOn("cmakeConfigure")
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }
        commandLine("cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath, "--config", cmakeBuildType)
    }

    tasks.register<Exec>("cmakeClean") {
        group = "clean"
        workingDir(cmakeBuildDir)
        onlyIf { cmakeBuildDir.exists() }
        commandLine("cmd", "/c", "cmake.exe", "--build", cmakeBuildDir.absolutePath, "--target", "clean")
    }

    tasks.named("cinteropBleAdvertiseMingwX64") { dependsOn("cmakeBuild") }
    tasks.named("clean") { dependsOn("cmakeClean") }

    val copyBleAdvertiseDllToKne = tasks.register<Copy>("copyBleAdvertiseDllToKne") {
        group = "kne"
        description = "Copies ble_advertise.dll to the kne resource directory for bundling"
        from(layout.buildDirectory.dir("cmake/bin/Release"))
        from(layout.buildDirectory.dir("cmake/bin/Debug"))
        include("ble_advertise.dll")
        into(layout.buildDirectory.dir("generated/kne/nativeLib/kne/native/win32-x64"))
        dependsOn("cmakeBuild")
        mustRunAfter("copyKneNativeLib")
    }

    tasks.named("jvmProcessResources") { dependsOn(copyBleAdvertiseDllToKne) }

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
