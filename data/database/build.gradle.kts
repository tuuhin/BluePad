plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.dx.code.quality)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.database"
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
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.framework)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.koin.annotations)
            implementation(libs.androidx.room.runtime)
            // local
            implementation(projects.core.common)
            implementation(projects.core.model)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.koin.test.junit)
            implementation(libs.bundles.testing.unit)
            // local
            implementation(projects.core.testing)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}
