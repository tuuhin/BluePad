import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kdroidfilter.nucleus)
    alias(libs.plugins.build.konfig)
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    // compose
    implementation(libs.cmp.runtime)
    implementation(libs.cmp.foundation)
    implementation(libs.cmp.ui)
    implementation(libs.cmp.material3)
    implementation(libs.cmp.components.resources)
    // logging
    implementation(libs.kermit)
    implementation(libs.kermit.koin)
    // koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    // kdroid filter
    implementation(libs.kdroidfilter.decorated.window)
    implementation(libs.kdroidfilter.decorated.window.material3)
    // compose app
    implementation(projects.composeApp)
}

nucleus.application {
    mainClass = "com.sam.bluepad.MainKt"

    jvmArgs += listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "-Dsun.misc.unsafe.allow=true",
    )

    nativeDistributions {
        // no osx or linux target for now
        targetFormats(TargetFormat.Msi, TargetFormat.AppX)
        packageName = "BluePad"
        packageVersion = "1.0.0"

        windows {
            console = false
            perUserInstall = true
            dirChooser = true
        }
    }
}



compose.resources {

    publicResClass = false
    packageOfResClass = "com.sam.bluepad.desktop.resources"
    generateResClass = auto

    // for jvm specific resources
    customDirectory(
        sourceSetName = "main",
        directoryProvider = provider {
            layout.projectDirectory.dir("src").dir("main").dir("resources")
                .dir("composeResources")
        },
    )
}

