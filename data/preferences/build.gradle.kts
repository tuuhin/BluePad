plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.wire.plugin)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.preferences"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.bluepad.testing.InstrumentTestRunner"
            execution = "HOST"
        }
    }

    jvm()

    sourceSets {

        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.koin.annotations)
            implementation(libs.wire.runtime)
            implementation(libs.kotlinx.serialization.protobuf)
            // local
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.domain)
            // datastore
            implementation(libs.bundles.datastore)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.koin.test.junit)
            implementation(libs.bundles.testing.unit)
            // local
            implementation(projects.core.testing)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}
