package modular.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `kmp.core`: a shared Kotlin Multiplatform module, for `shared/core/<name>`.
 *
 * Targets: Android and iOS (device and Apple silicon simulator).
 * Tests: `commonTest` runs on the JVM with `./gradlew :<module>:testAndroidHostTest`.
 */
class KmpCorePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            extensions.configure(KotlinMultiplatformExtension::class.java) {
                // Every declaration states its visibility: what is `public` here is what Swift and Compose see.
                explicitApi()

                // The Android target is declared by the Android plugin as an extension of `kotlin {}`.
                (this as ExtensionAware).extensions.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) {
                    namespace = moduleNamespace
                    compileSdk = libs.intVersion("android-compileSdk")
                    minSdk = libs.intVersion("android-minSdk")
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                    // Host tests share the `test` tree: they compile and run commonTest on the JVM.
                    withHostTestBuilder {
                        sourceSetTreeName = "test"
                    }
                }

                iosArm64()
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation(libs.library("kotlinx-coroutines-core"))
                }
                sourceSets.commonTest.dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.library("kotlinx-coroutines-test"))
                }
            }

            registerModuleRulesCheck()
        }
    }
}
