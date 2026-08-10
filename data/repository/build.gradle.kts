plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.repository"
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
            // local
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.domain)
            implementation(projects.data.database)
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
