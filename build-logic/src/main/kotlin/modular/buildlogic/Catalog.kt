package modular.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

/** Base package of the project. Placeholder: rename it once the application id is decided. */
const val BASE_PACKAGE = "com.example.modular"

/** The version catalog of the main build: gradle/libs.versions.toml. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalArgumentException("No library '$alias' in gradle/libs.versions.toml")
    }

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).orElseThrow {
        IllegalArgumentException("No version '$alias' in gradle/libs.versions.toml")
    }.requiredVersion.toInt()

/**
 * Android namespace of a module, derived from its Gradle path so that it never has to be written:
 * `:shared:feature:login` gives `com.example.modular.shared.feature.login`.
 * Bazel imports the module under the same name: see android/shared/BUILD.bazel.
 */
internal val Project.moduleNamespace: String
    get() = (listOf(BASE_PACKAGE) + pathSegments).joinToString(".")

/**
 * Maven group of a module: its namespace without its own name.
 * `:shared:feature:login` is published as `com.example.modular.shared.feature:login-android`.
 */
internal val Project.moduleGroup: String
    get() = (listOf(BASE_PACKAGE) + pathSegments.dropLast(1)).joinToString(".")

private val Project.pathSegments: List<String>
    get() = path.split(':').filter { it.isNotEmpty() }
