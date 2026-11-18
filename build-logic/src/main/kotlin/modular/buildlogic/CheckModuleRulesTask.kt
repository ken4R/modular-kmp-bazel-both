package modular.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Fails when the module declares a dependency that [ModuleRules] forbids. */
abstract class CheckModuleRulesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencyPaths: SetProperty<String>

    @TaskAction
    fun check() {
        val violations = ModuleRules.violations(modulePath.get(), dependencyPaths.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module dependency rules are broken:\n" + violations.joinToString("\n") { "  - $it" } +
                    "\nThe rules are listed in build-logic/src/main/kotlin/modular/buildlogic/ModuleRules.kt."
            )
        }
    }
}

/** Registers `checkModuleRules` and makes `check` run it. */
internal fun Project.registerModuleRulesCheck() {
    val modulePathValue = path
    val checkModuleRules = tasks.register("checkModuleRules", CheckModuleRulesTask::class.java) {
        group = "verification"
        description = "Checks that this module only depends on modules its type allows."
        modulePath.set(modulePathValue)
        dependencyPaths.set(
            provider {
                configurations.flatMap { configuration ->
                    configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
                }.toSet()
            }
        )
    }
    tasks.named("check") { dependsOn(checkModuleRules) }
}
