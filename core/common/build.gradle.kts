plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.common"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.kermit)
            api(libs.kotlinx.coroutines.core)

            implementation(libs.koin.annotations)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
