import io.github.kdroidfilter.nucleus.desktop.application.dsl.CompressionLevel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kdroidfilter.nucleus)
    alias(libs.plugins.build.konfig)
}

val operatingSystem: OperatingSystem = OperatingSystem.current()
val arch: String = System.getProperty("os.arch")

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
    implementation(libs.kdroidfilter.core.runtime)
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

    buildTypes {
        release {
            proguard {
                isEnabled = true
                optimize = true
                obfuscate = false
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }

    nativeDistributions {
        // no osx or linux target for now
        targetFormats(TargetFormat.Msi, TargetFormat.AppX)
        appName = "BluePad"
        packageName = "BluePad"
        packageVersion = "0.1.0"
        description =
            "A desktop app to send sketches via bluetooth to the receiver application can be any other targets"

        modules("java.instrument", "jdk.unsupported")

        outputBaseDir.set(project.layout.buildDirectory.dir("desktop"))
        appResourcesRootDir.set(project.layout.projectDirectory.dir("desktopResources"))
        licenseFile.set(rootProject.file("LICENSE"))

        compressionLevel = CompressionLevel.Maximum
        artifactName = "${name}-${version}-${operatingSystem.name}-${arch}.${ext}"

        cleanupNativeLibs = true

        windows {
            iconFile.set(appResourcesRootDir.file("windows/icons.ico"))
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

    customDirectory(
        sourceSetName = "main",
        directoryProvider = provider {
            layout.projectDirectory.dir("src/main/resources/composeResources")
        },
    )
}

