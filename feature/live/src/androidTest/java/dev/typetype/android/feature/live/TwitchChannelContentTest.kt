package dev.typetype.android.feature.live

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.twitch.TwitchChannel
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TwitchChannelContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theHeaderShowsFollowersAndTheTabsSwitch() {
        val chosen = AtomicReference<TwitchChannelTab>()
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    TwitchChannelContent(
                        state = TwitchChannelState(channel = channel(), isLoading = false),
                        onPlayVideo = {},
                        onSelect = chosen::set,
                        onRetry = {},
                        onLoadMore = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Space").assertIsDisplayed()
        composeRule.onNodeWithText("1.9M followers").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
        composeRule.onNodeWithText("Clips").performClick()

        assertEquals(TwitchChannelTab.Clips, chosen.get())
    }

    private fun channel() = TwitchChannel(
        login = "space",
        displayName = "Space",
        avatarUrl = null,
        description = "",
        bannerUrl = null,
        followers = 1_910_637,
        live = null,
    )
}
