import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.composeHotReload)
	alias(libs.plugins.ksp)
	alias(libs.plugins.androidx.room)
	alias(libs.plugins.kotlinx.serialization)
}

kotlin {
	@Suppress("DEPRECATION")
	androidTarget()
	jvm()

	sourceSets {
		androidMain.dependencies {
			implementation(compose.preview)
			implementation(libs.androidx.activity.compose)
			// database
			implementation(libs.androidx.room.sqlite.wrapper)
			// splash api
			implementation(libs.androidx.splash)
			// koin-di-android
			implementation(libs.koin.android)
			implementation(libs.koin.compose)
			implementation(libs.koin.android.startup)
		}
		commonMain.dependencies {
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			implementation(libs.compose.material3)
			implementation(libs.androidx.lifecycle.viewmodelCompose)
			implementation(libs.androidx.lifecycle.runtimeCompose)
			// room database
			implementation(libs.androidx.room.runtime)
			implementation(libs.androidx.sqlite.bundled)
			//di
			implementation(libs.koin.core)
			implementation(libs.koin.compose)
			implementation(libs.koin.compose.viewmodel)
			// navigation
			implementation(libs.jetbrains.navigation3.ui)
			implementation(libs.jetbrains.material3.adaptiveNavigation3)
			implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
			// kotlinx datetime and immutables
			implementation(libs.kotlinx.datetime)
			implementation(libs.kotlinx.collections.immutable)
			// crypto
			implementation(libs.kotlin.crypto.sha2)
			// logging
			implementation(libs.kermit)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.koin.test)
		}
		jvmMain.dependencies {
			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutinesSwing)
			implementation(project(":jvm-core:ble-common"))
			implementation(project(":jvm-core:simplejavable"))
			implementation(project(":jvm-core:ble-advertise"))
			implementation("io.ultreia:bluecove:2.1.1")
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
		optIn.add("kotlin.uuid.ExperimentalUuidApi")
		optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
	}
}

android {
	namespace = "com.sam.bluepad"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "com.sam.bluepad"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = 1
		versionName = "1.0.0"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

room {
	schemaDirectory("$projectDir/schemas")
}

dependencies {
	"kspAndroid"(libs.androidx.room.compiler)
	"kspJvm"(libs.androidx.room.compiler)
	debugImplementation(compose.uiTooling)
}

compose.desktop {
	application {
		mainClass = "com.sam.bluepad.MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "com.sam.bluepad"
			packageVersion = "1.0.0"
		}
	}
}

compose.resources {

	publicResClass = false
	packageOfResClass = "com.sam.bluepad.resources"
	generateResClass = auto

	// for jvm specific resources
	customDirectory(
		sourceSetName = "jvmMain",
		directoryProvider = provider {
			layout.projectDirectory.dir("src").dir("jvmMain").dir("resources")
				.dir("desktopResources")
		}
	)
}