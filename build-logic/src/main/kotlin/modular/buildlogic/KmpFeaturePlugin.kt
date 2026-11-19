package modular.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `kmp.feature`: the shared logic of one feature, for `shared/feature/<name>`.
 *
 * Same setup as `kmp.core`, plus the AndroidX ViewModel: a feature exposes its ViewModels,
 * their states and their events, and keeps the rest `internal`.
 */
class KmpFeaturePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpCorePlugin::class.java)

            extensions.configure(KotlinMultiplatformExtension::class.java) {
                sourceSets.commonMain.dependencies {
                    // `api`: the ViewModel base class is part of what a feature exposes.
                    api(libs.library("androidx-lifecycle-viewmodel"))
                }
            }
        }
    }
}
