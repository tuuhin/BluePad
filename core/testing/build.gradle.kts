plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.core.testing"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.bluepad.common.InstrumentTestRunner"
            execution = "HOST"
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.junit)
            implementation(libs.bundles.testing.android)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.bundles.testing.unit)
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))

            implementation(projects.core.common)
        }

        jvmMain.dependencies {
            api(libs.junit)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
