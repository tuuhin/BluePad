package com.sam.bluepad.plugins

import com.diffplug.gradle.spotless.SpotlessExtension
import com.sam.bluepad.plugins.ext.catalog
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KTCodeQualityPlugin : Plugin<Project> {


    override fun apply(target: Project) {
        target.applyPlugins()
        target.configureSpotless()
        target.configureDetekt()
    }

    private fun Project.applyPlugins() {
        val aliases = listOf("detekt", "spotless")
        aliases.forEach {
            catalog.findPlugin(it).ifPresent { libraryProvider ->
                val plugin = libraryProvider.get()
                plugins.apply(plugin.pluginId)
            }
        }
    }

    private fun Project.configureDetekt() = configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        allRules.set(true)

        source.setFrom(
            files(
                // common main
                "src/commonMain/kotlin",
                // desktop targets
                "src/jvmMain/kotlin",
                "src/macosMain/kotlin",
                "src/mingwMain/kotlin",
                "src/desktopMain/kotlin",
                // normal entry point
                "src/main/kotlin",
            ),
        )
    }


    private fun Project.configureSpotless() = configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint()
            trimTrailingWhitespace()
            endWithNewline()
        }

        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/**/*.gradle.kts")
            ktlint()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
