package modular.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `android.core`: a Compose library shared by the Android features, for `android/core/<name>`
 * (design system, common components, navigation).
 */
class AndroidCorePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure(LibraryExtension::class.java) {
                namespace = moduleNamespace
                compileSdk = libs.intVersion("android-compileSdk")
                defaultConfig {
                    minSdk = libs.intVersion("android-minSdk")
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures {
                    compose = true
                }
                testOptions {
                    // Local tests run against the same SDK level as the application.
                    targetSdk = libs.intVersion("android-targetSdk")
                }
            }

            addComposeDependencies()
            registerModuleRulesCheck()
        }
    }
}

/** Compose dependencies common to every Android UI module. Versions come from the Compose BOM. */
internal fun Project.addComposeDependencies() {
    dependencies.apply {
        add("implementation", platform(libs.library("androidx-compose-bom")))
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-material3"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    }
}
