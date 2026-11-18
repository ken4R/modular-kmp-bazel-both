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
    fun sharedCoreMustNotDependOnAFeature() {
        assertViolation(":shared:core:session", ":shared:feature:login")
    }

    @Test
    fun sharedCodeMustNotDependOnTheUmbrella() {
        assertViolation(":shared:core:session", ":shared:umbrella")
        assertViolation(":shared:feature:login", ":shared:umbrella")
    }

    @Test
    fun umbrellaMayDependOnSharedModules() {
        assertNoViolation(":shared:umbrella", ":shared:feature:login", ":shared:core:session")
    }

    @Test
    fun moduleAtAnUnknownLocationIsRejected() {
        assertEquals(1, ModuleRules.violations(":shared:login", emptyList()).size)
        assertEquals(1, ModuleRules.violations(":android:app", emptyList()).size)
        assertViolation(":shared:umbrella", ":misc:utils")
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
