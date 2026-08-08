import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.benchmarks)
    alias(libs.plugins.kotlin.all.open)
    alias(libs.plugins.nucleus.nna)
    alias(libs.plugins.nucleus.nna.cmake.ext)
}

val currentOs: OperatingSystem = OperatingSystem.current()

group = "com.sam.bluepad.native_compression"

kotlin {

    jvmToolchain(22)

    when {
        currentOs.isWindows -> mingwX64 {
            compilations.getByName("main").cinterops.create("kompression") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/windows_compression.def"))
                packageName = "com.sam.bluepad.compression.native.mingw"
                includeDirs(rootProject.file("cpp/windows/compression/include"))
                binaries.all {
                    linkerOpts("-lnative_compression")
                }
            }
        }

        currentOs.isMacOsX -> macosArm64 {
            compilations.getByName("main").cinterops.create("kompression") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/macos_compression.def"))
                packageName = "com.sam.bluepad.compression.native.macos"
            }
        }

        currentOs.isLinux -> linuxX64()
    }

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.lz4.java)
            implementation(libs.zstd.jni)
            implementation(libs.kotlinx.benchmark.runtime)
        }
        jvmTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlin.test)
        }

    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

benchmark {
    configurations {
        getByName("main") {
            warmups = 5
            iterations = 5
            iterationTime = 3
            iterationTimeUnit = "s"
        }
    }
    targets {
        register("jvm")
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlinNativeExportCmakeExt {
    nativeLibName.set("kompressionNative")
    generatedPackageName.set("com.sam.bluepad.compression.native")
    cmakeFilePath.set(rootProject.file("cpp/windows/compression"))

    val envNativeBuildType = providers.environmentVariable("NATIVE_BUILD_TYPE_RELEASE")
    val propertiesBuildType = providers.gradleProperty("cmake.buildTypeRelease")
    val isRelease = envNativeBuildType.getOrElse("false").toBoolean()
        || propertiesBuildType.getOrElse("false").toBoolean()

    releaseBuildEnabled.set(isRelease)
}
