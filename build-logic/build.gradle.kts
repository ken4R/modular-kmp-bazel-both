plugins {
    `kotlin-dsl`
}

dependencies {
    // Third-party Gradle plugins are applied by the convention plugins only,
    // so every module sees the same versions on a single classpath.
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.skie.gradle.plugin)

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
        register("androidCore") {
            id = "android.core"
            implementationClass = "modular.buildlogic.AndroidCorePlugin"
        }
        register("androidFeature") {
            id = "android.feature"
            implementationClass = "modular.buildlogic.AndroidFeaturePlugin"
        }
        register("androidApp") {
            id = "android.app"
            implementationClass = "modular.buildlogic.AndroidAppPlugin"
        }
    }
}
