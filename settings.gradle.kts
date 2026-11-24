pluginManagement {
    // Convention plugins: one per module type. AGENTS.md lists them.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "modular-kmp-bazel-both"

// Shared code (Kotlin Multiplatform). A feature never depends on another feature.
// Gradle builds nothing else: Bazel builds the Android and iOS applications from what it publishes.
include(":shared:core:session")
include(":shared:feature:login")
include(":shared:umbrella")
