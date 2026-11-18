package modular.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `android.app`: the Android application, for `android/app`.
 * It is the only Android module allowed to depend on every other module.
 */
class AndroidAppPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure(ApplicationExtension::class.java) {
                namespace = moduleNamespace
                compileSdk = libs.intVersion("android-compileSdk")
                defaultConfig {
                    applicationId = BASE_PACKAGE
                    minSdk = libs.intVersion("android-minSdk")
                    targetSdk = libs.intVersion("android-targetSdk")
                    versionCode = 1
                    versionName = "0.1.0"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures {
                    compose = true
                }
            }

            addComposeDependencies()
            dependencies.apply {
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("implementation", libs.library("androidx-activity-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            }

            registerModuleRulesCheck()
        }
    }
}
