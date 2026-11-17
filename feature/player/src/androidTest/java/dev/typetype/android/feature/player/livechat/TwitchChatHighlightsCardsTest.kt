package dev.typetype.android.feature.player.livechat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchPoll
import dev.typetype.android.domain.twitch.TwitchPollChoice
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TwitchChatHighlightsCardsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPollShowsEachChoiceShareAndCanBeHidden() {
        val hidden = AtomicReference<String>()
        val poll = TwitchPoll(
            id = "p1",
            title = "Next boss?",
            votes = 4,
            choices = listOf(TwitchPollChoice("a", "Lion", 3), TwitchPollChoice("b", "Dragon", 1)),
        )
        composeRule.setContent {
            TypeTypeTheme { TwitchChatHighlightsCards(TwitchChatHighlights(poll, null), onHide = hidden::set) }
        }

        composeRule.onNodeWithText("Next boss?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Lion, 75%").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dragon, 25%").assertIsDisplayed()
        composeRule.onNodeWithText("4 votes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Hide").performClick()

        assertEquals("p1", hidden.get())
    }
}
