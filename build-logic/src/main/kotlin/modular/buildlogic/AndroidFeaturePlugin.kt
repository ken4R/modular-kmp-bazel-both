package modular.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `android.feature`: the Compose screens of one feature, for `android/feature/<name>`.
 *
 * Same setup as `android.core`, plus what a screen needs to observe a shared ViewModel,
 * and screen tests that run on the JVM with Robolectric: `./gradlew :<module>:testDebugUnitTest`.
 */
class AndroidFeaturePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidCorePlugin::class.java)

            extensions.configure(LibraryExtension::class.java) {
                testOptions {
                    unitTests {
                        // Robolectric needs the merged resources and manifest.
                        isIncludeAndroidResources = true
                    }
                }
            }

            dependencies.apply {
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))

                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-test-ext-junit"))
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                // Declares the empty activity that hosts the screen under test.
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
