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

rootProject.name = "modular-kmp-bazel"

// Shared code (Kotlin Multiplatform). A feature never depends on another feature.
include(":shared:core:session")
include(":shared:feature:login")
include(":shared:umbrella")

// Android application (Compose).
include(":android:app")
include(":android:core:designsystem")
include(":android:feature:login")
