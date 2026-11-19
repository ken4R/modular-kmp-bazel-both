package modular.buildlogic

import co.touchlab.skie.plugin.configuration.SkieExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

/** Name of the framework that iOS imports. Bazel, SPM and tools/shared/sync rely on it. */
const val SHARED_FRAMEWORK_NAME = "Shared"

/**
 * `kmp.umbrella`: the only module that produces the iOS framework, for `shared/umbrella`.
 *
 * It builds one static `Shared.xcframework` with SKIE:
 * `./gradlew :shared:umbrella:assembleSharedDebugXCFramework` (macOS only).
 * The modules visible from Swift are listed with [exportToSwift] in the module's build file.
 */
class KmpUmbrellaPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpCorePlugin::class.java)
            pluginManager.apply("co.touchlab.skie")

            extensions.configure(KotlinMultiplatformExtension::class.java) {
                val xcframework = XCFramework(SHARED_FRAMEWORK_NAME)
                targets.withType(KotlinNativeTarget::class.java).configureEach {
                    binaries.framework {
                        baseName = SHARED_FRAMEWORK_NAME
                        // Static: Kotlin is linked once into the app, and Bazel and SPM import the same file.
                        isStatic = true
                        binaryOption("bundleId", "$BASE_PACKAGE.shared")
                        xcframework.add(this)
                    }
                }
            }

            extensions.configure(SkieExtension::class.java) {
                // Build data stays on the developer's machine.
                analytics.disableUpload.set(true)
            }
        }
    }
}

/**
 * Makes [dependencies] part of the Swift API of the `Shared` framework: their declarations keep
 * their Kotlin names in Swift. Everything else stays reachable from Kotlin only.
 *
 * Usage, in `shared/umbrella/build.gradle.kts`:
 * ```
 * kotlin {
 *     exportToSwift(project(":shared:feature:login"))
 * }
 * ```
 */
fun KotlinMultiplatformExtension.exportToSwift(vararg dependencies: Any) {
    sourceSets.commonMain.dependencies {
        dependencies.forEach { api(it) }
    }
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        binaries.withType(Framework::class.java).configureEach {
            dependencies.forEach { export(it) }
        }
    }
}
