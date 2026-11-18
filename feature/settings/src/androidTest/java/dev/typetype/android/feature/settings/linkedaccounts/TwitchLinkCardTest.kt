package dev.typetype.android.feature.settings.linkedaccounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.twitch.TwitchLinkCode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TwitchLinkCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPendingCodeIsShownAndCanBeCancelled() {
        var cancelled = false
        val code = TwitchLinkCode("d1", "ABCD1234", "https://www.twitch.tv/activate?device-code=ABCD1234", 5, 1800)
        composeRule.setContent {
            TypeTypeTheme { TwitchLinkCardContent(TwitchLinkState(available = true, code = code), {}, { cancelled = true }, {}) }
        }

        composeRule.onNodeWithText("ABCD1234").assertIsDisplayed()
        composeRule.onNodeWithText("Open Twitch").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        assertTrue(cancelled)
    }

    @Test
    fun aLinkedAccountShowsItsNameAndOffersToDisconnect() {
        var disconnected = false
        composeRule.setContent {
            TypeTypeTheme {
                TwitchLinkCardContent(TwitchLinkState(available = true, linked = true, linkedName = "Space"), {}, {}, { disconnected = true })
            }
        }

        composeRule.onNodeWithText("Connected as Space").assertIsDisplayed()
        composeRule.onNodeWithText("Disconnect").performClick()

        assertTrue(disconnected)
    }
}
