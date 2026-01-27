import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
}

kotlin {
	jvmToolchain {
		this.languageVersion = JavaLanguageVersion.of(17)
	}

	androidLibrary {
		namespace = "com.sam.bluepad.library"
		compileSdk = libs.versions.android.compileSdk.get().toInt()

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
		commonMain.dependencies {
			implementation(libs.cmp.runtime)
			implementation(libs.cmp.foundation)
			implementation(libs.cmp.ui)
			implementation(libs.cmp.material3)
			implementation(libs.cmp.adaptive)
			implementation(libs.cmp.components.resources)
			implementation(libs.cmp.ui.tooling.preview)
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
			implementation(libs.kotlin.crypto.random)
			// logging
			implementation(libs.kermit)
			//data store
			implementation(libs.androidx.datastore)
			implementation(libs.androidx.datastore.preferences)
			// permissions
			implementation(libs.moko.permissions)
			// toast
			implementation(libs.compose.toast)
			// protobuf
			implementation(libs.kotlinx.serialization.core)
			implementation(libs.kotlinx.serialization.protobuf)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.koin.test)
		}
		jvmMain.dependencies {
			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutinesSwing)
			implementation(projects.jvmCore.bleCommon)
			implementation(projects.jvmCore.bleAdvertise)
			implementation(libs.bluecove)

			// kable ble scanning
			implementation(libs.kable.core)
			implementation(libs.kable.exceptions)
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
		optIn.add("kotlin.uuid.ExperimentalUuidApi")
		optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
		optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
	}
}

room {
	schemaDirectory("$projectDir/schemas")
}

composeCompiler {
	metricsDestination = layout.buildDirectory.dir("compose_compiler")
	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
	"kspAndroid"(libs.androidx.room.compiler)
	"kspJvm"(libs.androidx.room.compiler)
}

compose.desktop {
	application {
		mainClass = "com.sam.bluepad.MainKt"

		nativeDistributions {
			// no osx or linux target for now
			val targets = arrayOf(TargetFormat.Msi)
			targetFormats(*targets)
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

buildkonfig {
	packageName = "com.sam.bluepad"

	defaultConfigs {
		buildConfigField(FieldSpec.Type.STRING, "APP_ID", "e1e55e42-bb6c-4410-94e4-a2cc2e628c05")
		buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
	}
}