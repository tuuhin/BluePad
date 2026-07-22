import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.wire.plugin)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(22)

    android {
        namespace = "com.sam.bluepad.library"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.bluepad.InstrumentTestRunner"
            execution = "HOST"
        }

        androidResources {
            enable = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling.preview)
            // database
            implementation(libs.androidx.room.sqlite.wrapper)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }

        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.compose.navigation3)
            implementation(libs.cmp.adaptive)
            implementation(libs.cmp.ui.tooling.preview)
            // room database & datastore
            implementation(libs.bundles.room)
            implementation(libs.bundles.datastore)
            // di
            implementation(libs.bundles.koin.common)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose.nav3)
            // kotlinx utilities
            implementation(libs.bundles.kotlinx.common)
            // crypto & bluetooth
            implementation(libs.bundles.crypto)
            // logging & notifications
            implementation(libs.kermit)
            implementation(libs.moko.permissions)
            implementation(libs.compose.toast)
            // wire & serialization
            implementation(libs.wire.runtime)
            implementation(libs.kotlinx.serialization.protobuf)
            // colors
            implementation(libs.material.kolor)
        }

        commonTest.dependencies {
            // koin test
            implementation(libs.koin.test)
            implementation(libs.koin.test.junit)
            implementation(libs.koin.annotations)
            // testing bundle
            implementation(libs.bundles.testing.unit)
            implementation(libs.cmp.ui.test)
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))
            implementation(libs.kotlinx.coroutinesSwing)
            // kable ble scanning
            implementation(libs.bundles.kable)
            // local modules
            implementation(projects.jvmCore.btCommon)
            implementation(projects.jvmCore.bleAdvertise)
            implementation(projects.jvmCore.cryptoBridge)
            implementation(projects.jvmCore.commonUtility)
            // color
            implementation(libs.nucleus.system.accent)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.cmp.ui.test.junit)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

koinCompiler {
    userLogs = true
}


room {
    schemaDirectory("$projectDir/schemas")
}

composeCompiler {
//    metricsDestination = layout.buildDirectory.dir("compose_compiler")
//    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
    "kspAndroid"(libs.androidx.room.compiler)
    "kspJvm"(libs.androidx.room.compiler)
    "androidRuntimeClasspath"(libs.androidx.ui.tooling.preview)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.sam.bluepad.resources"
    generateResClass = auto

    // for jvm specific resources
    customDirectory(
        sourceSetName = "jvmMain",
        directoryProvider = provider {
            layout.projectDirectory.dir("src/jvmMain/resources/compose")
        },
    )
}

buildkonfig {
    packageName = "com.sam.bluepad"
    exposeObjectWithName = "BuildKonfig"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "APP_ID", "e1e55e42-bb6c-4410-94e4-a2cc2e628c05")
        buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}
