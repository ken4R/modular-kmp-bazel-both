package modular.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleRulesTest {

    @Test
    fun sharedFeatureMayDependOnSharedCore() {
        assertNoViolation(":shared:feature:login", ":shared:core:session")
    }

    @Test
    fun sharedFeatureMustNotDependOnAnotherFeature() {
        val violations = ModuleRules.violations(":shared:feature:login", listOf(":shared:feature:profile"))

        assertEquals(1, violations.size)
        assertTrue("A feature never depends on another feature" in violations.single())
    }

    @Test
    fun sharedCodeMustNotDependOnAndroidModules() {
        assertViolation(":shared:core:session", ":android:core:designsystem")
        assertViolation(":shared:feature:login", ":android:core:designsystem")
    }

    @Test
    fun sharedCoreMustNotDependOnAFeature() {
        assertViolation(":shared:core:session", ":shared:feature:login")
    }

    @Test
    fun androidFeatureMayDependOnItsOwnSharedFeatureAndOnCores() {
        assertNoViolation(
            ":android:feature:login",
            ":shared:feature:login",
            ":shared:core:session",
            ":android:core:designsystem",
        )
    }

    @Test
    fun androidFeatureMustNotDependOnAnotherFeature() {
        assertViolation(":android:feature:login", ":shared:feature:profile")
        assertViolation(":android:feature:login", ":android:feature:profile")
    }

    @Test
    fun androidFeatureMustNotDependOnTheUmbrella() {
        assertViolation(":android:feature:login", ":shared:umbrella")
    }

    @Test
    fun umbrellaMayDependOnSharedModulesOnly() {
        assertNoViolation(":shared:umbrella", ":shared:feature:login", ":shared:core:session")
        assertViolation(":shared:umbrella", ":android:core:designsystem")
    }

    @Test
    fun applicationMayDependOnEverything() {
        assertNoViolation(
            ":android:app",
            ":shared:umbrella",
            ":shared:feature:login",
            ":android:feature:login",
            ":android:core:designsystem",
        )
    }

    @Test
    fun moduleAtAnUnknownLocationIsRejected() {
        assertEquals(1, ModuleRules.violations(":shared:login", emptyList()).size)
        assertViolation(":android:app", ":misc:utils")
    }

    @Test
    fun dependencyOnItselfIsIgnored() {
        assertNoViolation(":shared:feature:login", ":shared:feature:login")
    }

    private fun assertNoViolation(module: String, vararg dependencies: String) {
        assertEquals(emptyList(), ModuleRules.violations(module, dependencies.toList()))
    }

    private fun assertViolation(module: String, dependency: String) {
        assertEquals(1, ModuleRules.violations(module, listOf(dependency)).size, "$module -> $dependency")
    }
}
