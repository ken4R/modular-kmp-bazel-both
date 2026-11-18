package modular.buildlogic

/**
 * Dependency rules between Gradle modules. The `checkModuleRules` task of every module applies them,
 * so a forbidden dependency fails the build instead of waiting for a review.
 *
 * | Module                    | May depend on                                                  |
 * |---------------------------|----------------------------------------------------------------|
 * | `:shared:core:*`          | `:shared:core:*`                                               |
 * | `:shared:feature:<name>`  | `:shared:core:*`                                               |
 * | `:shared:umbrella`        | `:shared:core:*`, `:shared:feature:*`                          |
 * | `:android:core:*`         | `:android:core:*`, `:shared:core:*`                            |
 * | `:android:feature:<name>` | `:shared:feature:<name>`, `:shared:core:*`, `:android:core:*`  |
 * | `:android:app`            | everything                                                     |
 */
object ModuleRules {

    /** Returns one message per forbidden dependency of [module]. Both sides are Gradle project paths. */
    fun violations(module: String, dependencies: Collection<String>): List<String> {
        val from = Kind.of(module)
            ?: return listOf(
                "$module is not at a known location. Expected :shared:core:<name>, :shared:feature:<name>, " +
                    ":shared:umbrella, :android:core:<name>, :android:feature:<name> or :android:app."
            )
        return dependencies
            .filter { it != module }
            .distinct()
            .sorted()
            .mapNotNull { dependency -> reasonAgainst(module, from, dependency) }
            .map { reason -> "$module: $reason" }
    }

    private fun reasonAgainst(module: String, from: Kind, dependency: String): String? {
        val to = Kind.of(dependency) ?: return "$dependency is not at a known location."
        val allowed = when (from) {
            Kind.SharedCore -> to == Kind.SharedCore
            Kind.SharedFeature -> to == Kind.SharedCore
            Kind.Umbrella -> to == Kind.SharedCore || to == Kind.SharedFeature
            Kind.AndroidCore -> to == Kind.AndroidCore || to == Kind.SharedCore
            Kind.AndroidFeature -> to == Kind.AndroidCore || to == Kind.SharedCore ||
                (to == Kind.SharedFeature && name(dependency) == name(module))
            Kind.AndroidApp -> true
        }
        if (allowed) return null
        return when {
            from.isFeature && to.isFeature ->
                "must not depend on $dependency. A feature never depends on another feature: " +
                    "move what they share to a core module."
            else -> "must not depend on $dependency. ${from.description} only depends on ${from.allowedDescription}."
        }
    }

    private fun name(path: String): String = path.substringAfterLast(':')

    private enum class Kind(val description: String, val allowedDescription: String) {
        SharedCore("A shared core module", "other shared core modules"),
        SharedFeature("A shared feature", "shared core modules"),
        Umbrella("The umbrella", "shared modules"),
        AndroidCore("An Android core module", "Android core modules and shared core modules"),
        AndroidFeature(
            "An Android feature",
            "its own shared feature, shared core modules and Android core modules",
        ),
        AndroidApp("The Android application", "any module");

        val isFeature: Boolean get() = this == SharedFeature || this == AndroidFeature

        companion object {
            fun of(path: String): Kind? {
                val parts = path.split(':').filter { it.isNotEmpty() }
                return when {
                    parts == listOf("shared", "umbrella") -> Umbrella
                    parts == listOf("android", "app") -> AndroidApp
                    parts.size == 3 && parts[0] == "shared" && parts[1] == "core" -> SharedCore
                    parts.size == 3 && parts[0] == "shared" && parts[1] == "feature" -> SharedFeature
                    parts.size == 3 && parts[0] == "android" && parts[1] == "core" -> AndroidCore
                    parts.size == 3 && parts[0] == "android" && parts[1] == "feature" -> AndroidFeature
                    else -> null
                }
            }
        }
    }
}
