import io.github.kdroidfilter.nucleus.desktop.application.dsl.CompressionLevel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    // kotlinx datetime
    implementation(libs.kotlinx.datetime)
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
                version = "7.9.1"
                isEnabled = false
                optimize = true
                obfuscate = false
                joinOutputJars = true
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }

    nativeDistributions {
        // no osx or linux target for now
        targetFormats(TargetFormat.Msi, TargetFormat.AppX, TargetFormat.Dmg)
        appName = "BluePad"
        packageName = "BluePad"
        packageVersion = "1.0.0"
        description =
            "BluePad is a offline-first sketch and idea synchronization app. It allows users to securely sync text-based " +
                "sketches between their own nearby devices without relying on the internet, cloud services, or user accounts, but using only bluetooth"

        modules("java.instrument", "jdk.unsupported")

        outputBaseDir.set(project.layout.buildDirectory.dir("desktop"))
        appResourcesRootDir.set(project.layout.projectDirectory.dir("desktopResources"))
        licenseFile.set(rootProject.file("LICENCE"))

        compressionLevel = CompressionLevel.Normal
        artifactName = "${name}-${version}-${operatingSystem.name}-${arch}.${ext}"

        // strips non targets file from dependency jar
        cleanupNativeLibs = true

        windows {
            iconFile.set(appResourcesRootDir.file("windows/icons.ico"))
            console = false
            perUserInstall = true
            dirChooser = true
        }

        macOS {
            bundleID = "com.sam.bluepad.app"
            dockName = "Bluepad"
            appCategory = "public.app-category.productivity"

            minimumSystemVersion = "12.0"
            macOsSdkVersion = "26.0"

            installationPath = "/Applications"

            layeredIconDir.set(appResourcesRootDir.dir("macos/icons/BluePad.icon"))
            iconFile.set(appResourcesRootDir.file("macos/icons/BluePad.icns"))

            entitlementsFile.set(appResourcesRootDir.file("macos/entitlements.plist"))
            runtimeEntitlementsFile.set(appResourcesRootDir.file("macos/runtime-entitlements.plist"))

            infoPlist {
                extraKeysRawXml = """
                <key>CFBundleDisplayName</key>
                <string>BluePad</string>
                <key>NSBluetoothAlwaysUsageDescription</key>
                <string>BluePad uses Bluetooth to discover and sync notes between your devices.</string>
                <key>NSBluetoothPeripheralUsageDescription</key>
                <string>BluePad uses Bluetooth to communicate with nearby devices.</string>
            """.trimIndent()
            }

            dmg {
                title = "$packageName $version"
                badgeIcon.set(appResourcesRootDir.file("macos/icons/BluePad.icns"))
                backgroundColor = "#90CBFF"

                iconSize = 128
                iconTextSize = 12

                window {
                    x = 400
                    y = 100
                    width = 640
                    height = 420
                }
            }
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


tasks.withType<Test>().configureEach { setupNativePathForMingw() }
tasks.withType<JavaExec>().configureEach { setupNativePathForMingw() }

/**
 * We need to set the paths for the native library as our library has a transitive dependency on another `.dll`
 * As windows is unable to resolve the path we need to provide the path
 */
private fun Task.setupNativePathForMingw() {
    if (!operatingSystem.isWindows) return

    val binDirs = mutableListOf<String>()

    for (subproject in rootProject.subprojects) {
        if (!subproject.path.startsWith(":jvm-core")) continue
        val kotlinExp = subproject.extensions.findByType<KotlinMultiplatformExtension>()
        val targets = kotlinExp?.targets ?: continue
        val mingwTarget = targets.findByName("mingwX64") as? KotlinNativeTarget

        mingwTarget?.binaries?.forEach { binary ->
            binDirs.add(binary.outputFile.parentFile.absolutePath)
        }
    }

    val existingPath = System.getenv("PATH") ?: ""
    val pathDelimiter = ";"
    val newPath = (existingPath.split(pathDelimiter) + binDirs.distinct())
        .distinct().joinToString(pathDelimiter)

    // update the enviroment path
    if (this is ProcessForkOptions) environment("PATH", newPath)
}

