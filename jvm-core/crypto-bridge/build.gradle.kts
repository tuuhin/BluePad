plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kdroidfilter.nucleusnativeaccess)
}
kotlin {

    jvmToolchain(25)

    mingwX64 {
        compilations.getByName("main") {
            cinterops.create("crypto") {
                definitionFile = project.file("src/nativeInterop/cinterop/windows_dpapi.def")
                packageName = "com.sam.bluepad.windows.dpapi"
            }
        }
    }

    // TODO: Include other platforms later

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
    buildType = "release"
}
