package dev.typetype.android.feature.settings.rss

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.server.RssCapability
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RssFeedsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyScreenOffersFeedCreation() {
        show(
            RssFeedsState(
                capability = RssCapability(true, 5, 80, 15, 30),
                availableServiceIds = setOf(0, 5, 6),
                isLoading = false,
                hasLoadedFeeds = true,
            ),
        )

        composeRule.onNodeWithText("No private RSS feeds yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Create a feed").assertIsDisplayed()
    }

    @Test
    fun serverFeedLimitPreventsAnotherCreation() {
        show(
            RssFeedsState(
                capability = RssCapability(true, 1, 80, 15, 30),
                feeds = listOf(feed()),
                isLoading = false,
                hasLoadedFeeds = true,
            ),
        )

        composeRule.onNodeWithText("Create a feed").assertIsNotEnabled()
    }

    @Test
    fun selectedChannelEditorSendsTheChosenSubscription() {
        val action = AtomicReference<RssFeedsAction>()
        show(
            state = RssFeedsState(
                capability = RssCapability(enabled = true),
                availableServiceIds = setOf(0),
                subscriptions = listOf(
                    SubscriptionSummary("https://youtube.com/@demo", "Demo channel", "", 1),
                ),
                isLoading = false,
                editor = RssFeedEditorState(
                    scope = RssFeedScope.Channels,
                    serviceIds = setOf(0),
                ),
            ),
            onAction = action::set,
        )

        composeRule.onNodeWithText("Demo channel").performClick()

        assertEquals(
            RssFeedsAction.ToggleChannel("https://youtube.com/@demo"),
            action.get(),
        )
    }

    @Test
    fun privateLinkIsShownWhenSecretStateIsPresent() {
        val url = "https://example.test/rss/private"
        show(
            RssFeedsState(
                secret = RssFeedSecretState("My subscriptions", url),
                isLoading = false,
            ),
        )
        composeRule.onNodeWithText(url).assertIsDisplayed()
    }

    @Test
    fun regenerateRequiresConfirmation() {
        val action = AtomicReference<RssFeedsAction>()
        show(
            RssFeedsState(
                feeds = listOf(feed()),
                regeneratingFeedId = "feed",
                isLoading = false,
            ),
            onAction = action::set,
        )

        composeRule.onNodeWithText("Regenerate private link").performClick()

        assertEquals(RssFeedsAction.ConfirmRegenerate, action.get())
    }

    @Test
    fun channelLimitDisablesAnotherSelection() {
        show(
            RssFeedsState(
                subscriptions = listOf(
                    SubscriptionSummary("https://youtube.com/@extra", "Extra channel", "", 1),
                ),
                editor = RssFeedEditorState(
                    scope = RssFeedScope.Channels,
                    channelUrls = (1..100).map { "https://youtube.com/@channel$it" }.toSet(),
                    serviceIds = setOf(0),
                ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("100 of 100 channels selected").assertIsDisplayed()
        composeRule.onNodeWithText("Extra channel").assertIsNotEnabled()
    }

    private fun show(
        state: RssFeedsState,
        onAction: (RssFeedsAction) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                RssFeedsScreen(state, onNavigateBack = {}, onAction = onAction)
            }
        }
    }

    private fun feed() = RssFeed(
        id = "feed",
        name = "My feed",
        scope = RssFeedScope.All,
        channelUrls = emptyList(),
        serviceIds = setOf(0),
        includeVideos = true,
        includeShorts = true,
        includeLive = true,
        includeUpcoming = true,
        enabled = true,
        createdAt = 1,
        updatedAt = 2,
        lastUsedAt = null,
    )
}
