import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.DmgContentType
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.nucleus.framework)
    alias(libs.plugins.nucleus.build.ext)
}

val osName: OperatingSystem = OperatingSystem.current()

kotlin {
    jvmToolchain(22)
}

dependencies {
    // compose ui
    implementation(libs.bundles.compose.ui)
    // logging & koin
    implementation(libs.kermit)
    implementation(libs.kermit.koin)
    implementation(libs.bundles.koin.common)
    // kotlinx datetime
    implementation(libs.kotlinx.datetime)
    // nucleus framework
    implementation(libs.bundles.nucleus)
    // compose app module
    implementation(projects.composeApp)
    //koin
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit)
    // compose ui test
    testImplementation(libs.cmp.ui.test)
    testImplementation(libs.cmp.ui.test.junit)
    // testing
    testImplementation(libs.bundles.testing.unit)
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

        // JVM ARGS SPECIFIC TO DISTRIBUTION
        jvmArgs += listOf($$"-Djava.library.path=$APPDIR/resources/libs")

        val commonProperties = Properties().apply {
            val commons = project.file("packaging.properties")
            commons.inputStream().use(::load)
        }

        // application targets
        targetFormats(
            // windows formats
            TargetFormat.Msi, TargetFormat.Portable, TargetFormat.Nsis,
            // macOS formats
            TargetFormat.Dmg,
        )

        // target base config
        appName = commonProperties.getProperty("APP_NAME")
        packageName = commonProperties.getProperty("APP_PACKAGE_NAME")
        packageVersion = commonProperties.getProperty("APP_PACKAGE_VERSION")
        description = commonProperties.getProperty("APP_DESCRIPTION")
        vendor = commonProperties.getProperty("APP_VENDOR")
        copyright = commonProperties.getProperty("APP_COPYRIGHT")
        licenseFile.set(rootProject.file("LICENCE"))

        // java modules
        modules("java.instrument", "jdk.unsupported", "java.management")

        // target common configuration
        outputBaseDir.set(project.layout.buildDirectory.dir("desktop"))
        appResourcesRootDir.set(project.layout.projectDirectory.dir("desktopResources"))
        compressionLevel = CompressionLevel.Normal
        cleanupNativeLibs = true

        // packaging configs
        val packagingRoot = project.layout.projectDirectory.dir("packaging")

        if (osName.isWindows) windows {
            iconFile.set(packagingRoot.file("windows/bluepad-1024.ico"))
            upgradeUuid = commonProperties.getProperty("WINDOWS_UPGRADE_UUID", null)?.ifEmpty { null }
            console = false
            perUserInstall = true
            dirChooser = true

            nsis {
                oneClick = false
                allowToChangeInstallationDirectory = true
                allowElevation = false
                perMachine = false
                createDesktopShortcut = true
                createStartMenuShortcut = true
                runAfterFinish = true
                deleteAppDataOnUninstall = false
                installerIcon.set(packagingRoot.file("windows/bluepad-1024.ico"))
                includeScript.set(packagingRoot.file("windows/nsis/packaging_install.nsh"))
            }

            appx {

                applicationId = commonProperties.getProperty("APP_PACKAGE_NAME")
                publisherDisplayName = commonProperties.getProperty("APP_VENDOR")
                displayName = commonProperties.getProperty("APP_NAME")
                publisher = commonProperties.getProperty("WINDOWS_APPX_PUBLISHER")
                identityName = commonProperties.getProperty("APP_PACKAGE_NAME")

                languages = listOf("en-US")
                backgroundColor = commonProperties.getProperty("APP_INSTALLER_BACKGROUND")
                showNameOnTiles = true
                addAutoLaunchExtension = false
                setBuildNumber = true

                storeLogo.set(packagingRoot.file("windows/appx/StoreLogo.png"))
                square44x44Logo.set(packagingRoot.file("windows/appx/Square44x44Logo.png"))
                square150x150Logo.set(packagingRoot.file("windows/appx/Square150x150Logo.png"))
                wide310x150Logo.set(packagingRoot.file("windows/appx/Wide310x150Logo.png"))
            }
        }
        else if (osName.isMacOsX) macOS {
            bundleID = commonProperties.getProperty("APP_PACKAGE_NAME")
            dockName = commonProperties.getProperty("APP_NAME")
            appCategory = "public.app-category.productivity"

            minimumSystemVersion = "12.0"
            macOsSdkVersion = "26.0"

            installationPath = "/Applications"

            layeredIconDir.set(packagingRoot.dir("macos/icons/BluePad.icon"))
            iconFile.set(packagingRoot.file("macos/icons/BluePad.icns"))

            entitlementsFile.set(packagingRoot.file("macos/entitlements.plist"))
            runtimeEntitlementsFile.set(packagingRoot.file("macos/runtime-entitlements.plist"))

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
                title =
                    "${commonProperties.getProperty("APP_NAME")} ${commonProperties.getProperty("APP_PACKAGE_VERSION")}"
                badgeIcon.set(packagingRoot.file("macos/icons/BluePad.icns"))
                backgroundColor = commonProperties.getProperty("APP_INSTALLER_BACKGROUND")

                iconSize = 128
                iconTextSize = 16

                window { x = 400; y = 100; width = 540; height = 380 }
                content(x = 130, y = 220, type = DmgContentType.File, name = "BluePad.app")
                content(x = 410, y = 220, type = DmgContentType.Link, path = "/Applications")
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


val generateMacosAppIcns = tasks.register<Exec>("genAppIconMacos") {
    group = "nucleus packaging"
    description = "Generates clipped, rounded macos icons"

    val icon = project.layout.projectDirectory.file("packaging/bluepad-1024.svg")
    val commonProperties = Properties().apply {
        val commons = project.file("packaging.properties")
        commons.inputStream().use(::load)
    }

    val script = project.layout.projectDirectory.file("packaging/macos/app_images.sh")
    val bgColor = commonProperties.getProperty("APP_INSTALLER_BACKGROUND", "#C4F18C")

    // Declare inputs & outputs for Gradle incremental build caching
    inputs.file(icon)
    inputs.file(script)
    inputs.property("bgColor", bgColor)
    outputs.dir(project.layout.projectDirectory.dir("packaging/macos/icons"))

    workingDir(project.layout.projectDirectory.file("packaging"))
    commandLine(
        "bash", script.asFile.absolutePath, icon.asFile.absolutePath,
        "-c", bgColor, "-r", "22", "-f", "85",
    )

}

val generateWindowsAppIcon = tasks.register<Exec>("genAppIconWindos") {
    group = "nucleus packaging"
    description = "Generates clipped, rounded Windows assets from the SVG source icon."

    val icon = project.layout.projectDirectory.file("packaging/bluepad-1024.svg")
    val commonProperties = Properties().apply {
        val commons = project.file("packaging.properties")
        commons.inputStream().use(::load)
    }

    val script = project.layout.projectDirectory.file("packaging/windows/appx_images.bat")
    val bgColor = commonProperties.getProperty("APP_INSTALLER_BACKGROUND", "#C4F18C")
    val outputDir = project.layout.projectDirectory.dir("packaging/windows/appx")

    // Declare inputs & outputs for Gradle incremental build caching
    inputs.file(icon)
    inputs.file(script)
    inputs.property("bgColor", bgColor)
    outputs.dir(outputDir)

    workingDir(project.layout.projectDirectory.file("packaging"))
    commandLine(
        "cmd", "/c", script.asFile.absolutePath, icon.asFile.absolutePath, outputDir.asFile.absolutePath,
        "-c", bgColor, "-r", "12",
    )
}

tasks.configureEach {
    if (osName.isMacOsX && (name.startsWith("package") || name.startsWith("createDist") || name.startsWith("compile"))) {
        dependsOn(generateMacosAppIcns)
    }
    if (osName.isWindows && (name.startsWith("package") || name.startsWith("createDist") || name.startsWith("compile"))) {
        dependsOn(generateWindowsAppIcon)
    }
}
