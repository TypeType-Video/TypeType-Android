package dev.typetype.android.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.feature.player.error.StreamErrorClass
import dev.typetype.android.feature.player.error.StreamErrorKind
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerErrorStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expiredAuthenticationExplainsRecoveryAndOpensAccounts() {
        val openedAccounts = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                ErrorState(
                    classification = StreamErrorClass(
                        kind = StreamErrorKind.AuthenticationExpired,
                        rawMessage = null,
                        requestId = "request-auth",
                    ),
                    onNavigateBack = {},
                    onRetry = {},
                    onOpenAccounts = { openedAccounts.set(true) },
                )
            }
        }

        composeRule.onNodeWithText(
            "Your session expired. Open accounts to sign in again or start a new guest session.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Request request-auth").assertIsDisplayed()
        composeRule.onNodeWithText("Open accounts").assertIsDisplayed().performClick()

        assertTrue(openedAccounts.get())
    }

    @Test
    fun terminalPreparationFailureOffersFreshSessionRetry() {
        val retried = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                ErrorState(
                    classification = StreamErrorClass(
                        kind = StreamErrorKind.SabrPreparationFailed,
                        rawMessage = null,
                        requestId = "request-playback",
                    ),
                    onNavigateBack = {},
                    onRetry = { retried.set(true) },
                    onOpenAccounts = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "The server could not prepare this video. Retry to start a fresh playback session.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Request request-playback").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()

        assertTrue(retried.get())
    }
}
