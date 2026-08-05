package dev.typetype.android.feature.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.feature.setup.login.LoginAction
import dev.typetype.android.feature.setup.login.LoginScreen
import dev.typetype.android.feature.setup.login.LoginState
import dev.typetype.android.feature.setup.register.RegisterAction
import dev.typetype.android.feature.setup.register.RegisterScreen
import dev.typetype.android.feature.setup.register.RegisterState
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SetupAuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bootstrapExplainsThatTheFirstAccountWillBeAdministrator() {
        showRegister(
            RegisterState(
                instanceName = "Family TV",
                isLoading = false,
                bootstrapAvailable = true,
                localLoginEnabled = true,
            ),
        )

        composeRule.onNodeWithText("Set up the first administrator account on Family TV.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Administrator name").assertIsDisplayed()
    }

    @Test
    fun closedRegistrationKeepsTheSignInPathAvailable() {
        val action = AtomicReference<RegisterAction>()
        showRegister(
            state = RegisterState(
                isLoading = false,
                localLoginEnabled = true,
            ),
            onAction = action::set,
        )

        composeRule.onAllNodesWithText("Registrations are currently closed.")
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Already have an account? Sign in").performClick()

        assertEquals(RegisterAction.OnBackClick, action.get())
    }

    @Test
    fun oidcOnlyRegistrationUsesTheServerProviderName() {
        showRegister(
            RegisterState(
                isLoading = false,
                oidcEnabled = true,
                oidcProviderName = "Example ID",
            ),
        )

        composeRule.onNodeWithText("Continue with Example ID").assertIsDisplayed()
        composeRule.onNodeWithText("Local registration is disabled on this instance.")
            .assertIsDisplayed()
    }

    @Test
    fun localRegistrationDisabledIsExplained() {
        showRegister(RegisterState(isLoading = false))

        composeRule.onNodeWithText("Local registration is disabled on this instance.")
            .assertIsDisplayed()
    }

    @Test
    fun loginRegistrationLinkDispatchesRegistrationAction() {
        val action = AtomicReference<LoginAction>()
        composeRule.setContent {
            TypeTypeTheme {
                LoginScreen(
                    state = LoginState(
                        isLoadingMethods = false,
                        localLoginEnabled = true,
                        registrationAllowed = true,
                    ),
                    onAction = action::set,
                )
            }
        }

        composeRule.onNode(hasText("Create account") and hasClickAction()).performClick()

        assertEquals(LoginAction.OnRegisterClick, action.get())
    }

    private fun showRegister(
        state: RegisterState,
        onAction: (RegisterAction) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                RegisterScreen(state = state, onAction = onAction)
            }
        }
    }
}
