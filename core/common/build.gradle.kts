plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
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
            api(libs.okio)
            api(libs.koin.core)
            api(libs.koin.annotations)

            // crypto
            implementation(libs.kotlin.crypto.sha2)
            implementation(libs.kotlin.crypto.random)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.koin.test.junit)
            implementation(libs.bundles.testing.unit)
            // local
            implementation(projects.core.testing)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
