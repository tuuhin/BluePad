import io.github.kdroidfilter.nucleus.desktop.application.dsl.CompressionLevel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.properties.Properties

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

        val commonProperties = Properties().apply {
            val commons = project.file("packaging.properties")
            commons.inputStream().use(::load)
        }

        // application targets
        targetFormats(TargetFormat.Msi, TargetFormat.Portable, TargetFormat.Nsis)

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

        // target common connfiguration
        outputBaseDir.set(project.layout.buildDirectory.dir("desktop"))
        appResourcesRootDir.set(project.layout.projectDirectory.dir("desktopResources"))
        compressionLevel = CompressionLevel.Normal
        cleanupNativeLibs = true

        // packaging configs
        val packagingRoot = project.layout.projectDirectory.dir("packaging")

        windows {
            iconFile.set(appResourcesRootDir.file("windows/icons.ico"))
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
                installerIcon.set(appResourcesRootDir.file("windows/icons.ico"))
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
    val operatingSystem: OperatingSystem = OperatingSystem.current()
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
