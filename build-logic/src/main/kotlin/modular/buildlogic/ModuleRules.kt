package modular.buildlogic

/**
 * Dependency rules between the Gradle modules: the shared code. The `checkModuleRules` task of every
 * module applies them, so a forbidden dependency fails the build instead of waiting for a review.
 * The Android and iOS modules are Bazel targets: Bazel visibility applies their rules.
 *
 * | Module                    | May depend on                          |
 * |---------------------------|----------------------------------------|
 * | `:shared:core:*`          | `:shared:core:*`                       |
 * | `:shared:feature:<name>`  | `:shared:core:*`                       |
 * | `:shared:umbrella`        | `:shared:core:*`, `:shared:feature:*`  |
 */
object ModuleRules {

    /** Returns one message per forbidden dependency of [module]. Both sides are Gradle project paths. */
    fun violations(module: String, dependencies: Collection<String>): List<String> {
        val from = Kind.of(module)
            ?: return listOf(
                "$module is not at a known location. Expected :shared:core:<name>, :shared:feature:<name> " +
                    "or :shared:umbrella."
            )
        return dependencies
            .filter { it != module }
            .distinct()
            .sorted()
            .mapNotNull { dependency -> reasonAgainst(from, dependency) }
            .map { reason -> "$module: $reason" }
    }

    private fun reasonAgainst(from: Kind, dependency: String): String? {
        val to = Kind.of(dependency) ?: return "$dependency is not at a known location."
        val allowed = when (from) {
            Kind.SharedCore -> to == Kind.SharedCore
            Kind.SharedFeature -> to == Kind.SharedCore
            Kind.Umbrella -> to == Kind.SharedCore || to == Kind.SharedFeature
        }
        if (allowed) return null
        return when {
            from == Kind.SharedFeature && to == Kind.SharedFeature ->
                "must not depend on $dependency. A feature never depends on another feature: " +
                    "move what they share to a core module."
            else -> "must not depend on $dependency. ${from.description} only depends on ${from.allowedDescription}."
        }
    }

    private enum class Kind(val description: String, val allowedDescription: String) {
        SharedCore("A shared core module", "other shared core modules"),
        SharedFeature("A shared feature", "shared core modules"),
        Umbrella("The umbrella", "shared modules");

        companion object {
            fun of(path: String): Kind? {
                val parts = path.split(':').filter { it.isNotEmpty() }
                return when {
                    parts == listOf("shared", "umbrella") -> Umbrella
                    parts.size == 3 && parts[0] == "shared" && parts[1] == "core" -> SharedCore
                    parts.size == 3 && parts[0] == "shared" && parts[1] == "feature" -> SharedFeature
                    else -> null
                }
            }
        }
    }
}
