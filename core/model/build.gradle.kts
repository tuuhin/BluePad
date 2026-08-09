plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.models"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.collections.immutable)
        }

    }
}
