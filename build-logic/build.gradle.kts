import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

/**
 * Maven coordinates of a Gradle plugin of the catalog: its marker artifact, which depends on the
 * plugin itself. The plugins are [plugins] of the catalog, not [libraries], because Bazel imports
 * every library of the catalog and must not resolve Gradle plugins.
 */
fun pluginArtifact(plugin: Provider<PluginDependency>): String =
    plugin.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

dependencies {
    // Third-party Gradle plugins are applied by the convention plugins only,
    // so every module sees the same versions on a single classpath.
    implementation(pluginArtifact(libs.plugins.kotlin.multiplatform))
    implementation(pluginArtifact(libs.plugins.android.kotlin.multiplatform.library))
    implementation(pluginArtifact(libs.plugins.skie))

    // Tests of the module rules: ./gradlew -p build-logic test
    testImplementation(kotlin("test"))
}

// One convention plugin per module type. A module applies exactly one of them.
gradlePlugin {
    plugins {
        register("kmpCore") {
            id = "kmp.core"
            implementationClass = "modular.buildlogic.KmpCorePlugin"
        }
        register("kmpFeature") {
            id = "kmp.feature"
            implementationClass = "modular.buildlogic.KmpFeaturePlugin"
        }
        register("kmpUmbrella") {
            id = "kmp.umbrella"
            implementationClass = "modular.buildlogic.KmpUmbrellaPlugin"
        }
    }
}
