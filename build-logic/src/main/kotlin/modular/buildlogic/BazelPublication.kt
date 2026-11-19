package modular.buildlogic

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

/**
 * Version of every shared module in the Maven repository of Bazel. The repository holds one build
 * of each module, so the version never changes: Bazel finds the libraries by path.
 */
const val SHARED_VERSION = "local"

/** Name of the Maven repository that Bazel reads, in Gradle task names. */
private const val BAZEL_REPOSITORY = "bazel"

/** Where that repository lives. It is built, never committed: see .gitignore. */
private const val BAZEL_REPOSITORY_PATH = "android/shared/maven"

/**
 * Publishes the Android side of a shared module for Bazel: its AAR and POM, in the local Maven
 * repository android/shared/maven. `./gradlew publishAndroidForBazel` publishes every shared module;
 * tools/shared/sync runs it, and android/shared/BUILD.bazel imports the libraries.
 *
 * Only the Android publication is published: the iOS side reaches Bazel as Shared.xcframework.
 */
internal fun Project.registerBazelPublication() {
    val repository = rootProject.layout.projectDirectory.dir(BAZEL_REPOSITORY_PATH).asFile

    extensions.configure(PublishingExtension::class.java) {
        repositories.maven {
            name = BAZEL_REPOSITORY
            url = repository.toURI()
        }
    }

    tasks.register("publishAndroidForBazel") {
        group = "publishing"
        description = "Publishes the Android library of this module to $BAZEL_REPOSITORY_PATH, for Bazel."
        dependsOn(
            tasks.withType(PublishToMavenRepository::class.java).matching { task ->
                task.repository.name == BAZEL_REPOSITORY && task.publication.name.startsWith("android")
            }
        )
    }
}
