package com.example.modular.feature.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.modular.core.designsystem.ModularTheme
import com.example.modular.shared.core.session.InMemorySessionRepository
import com.example.modular.shared.feature.login.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the screen with the real shared ViewModel, which Bazel loads from the AARs that Gradle
 * published: the test fails when the bridge between the two builds breaks.
 * `bazel test //android/feature/login:LoginRouteTest`.
 */
@RunWith(AndroidJUnit4::class)
class LoginRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModel = LoginViewModel(InMemorySessionRepository())

    @Test
    fun typingUpdatesTheSharedStateThatTheScreenShows() {
        composeRule.setContent {
            ModularTheme {
                LoginRoute(viewModel = viewModel, onSignedIn = {}, onPasswordResetRequested = {})
            }
        }
        composeRule.onNodeWithText("Se connecter").assertIsNotEnabled()

        composeRule.onNodeWithText("E-mail").performTextInput("ada@example.com")
        composeRule.onNodeWithText("Mot de passe").performTextInput("kotlin")

        assertEquals("ada@example.com", viewModel.state.value.email)
        composeRule.onNodeWithText("Se connecter").assertIsEnabled()
    }
}
