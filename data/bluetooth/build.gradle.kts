plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.bluetooth"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.koin.annotations)

            // local
            implementation(projects.core.domain)
        }

        jvmMain.dependencies {
            implementation(projects.jvmCore.btCommon)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
