package dev.typetype.android.feature.shorts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import dev.typetype.android.core.ui.branding.DeArrowBrandingEnvironment
import dev.typetype.android.core.ui.branding.LocalDeArrowBranding
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.branding.DeArrowPreferences
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShortsLoadingBudgetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brandingLoadsOnlyForTheReadyActiveShort() {
        val brandingRequests = AtomicInteger()
        val environment = DeArrowBrandingEnvironment(
            enabled = true,
            preferences = DeArrowPreferences("dearrow", "dearrow", "accepted"),
            loader = { _, _ ->
                brandingRequests.incrementAndGet()
                Result.success(null)
            },
        )
        var playbackReady by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAnimatedStatePlayback provides false,
                LocalDeArrowBranding provides environment,
            ) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = ShortsState(
                            videos = listOf(video("one"), video("two"), video("three")),
                            isLoading = false,
                        ),
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRefresh = {},
                        onLoadMore = {},
                        embeddedPlaybackEnabled = true,
                        playbackReady = playbackReady,
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(0, brandingRequests.get())

        composeRule.runOnUiThread { playbackReady = true }
        composeRule.waitUntil { brandingRequests.get() == 1 }

        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput { swipeUp() }
        composeRule.waitUntil { brandingRequests.get() == 2 }
        assertEquals(2, brandingRequests.get())
    }

    @Test
    fun swipeDoesNotRecomposeMetadataOnEveryFrame() {
        val statsEvaluations = AtomicInteger()
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = ShortsState(
                            videos = List(6) { video(it.toString()) },
                            isLoading = false,
                        ),
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRefresh = {},
                        onLoadMore = {},
                        embeddedPlaybackEnabled = true,
                        statsForVideo = {
                            statsEvaluations.incrementAndGet()
                            ShortsVideoStats(it.viewCount, null)
                        },
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val initialEvaluations = statsEvaluations.get()
        repeat(3) {
            composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }

        val swipeEvaluations = statsEvaluations.get() - initialEvaluations
        assertTrue("metadata evaluated $swipeEvaluations times", swipeEvaluations <= 10)
    }

    private fun video(id: String) = Video(
        id = id,
        url = "https://video/$id",
        title = "Short $id",
        thumbnailUrl = "https://image/$id",
        uploaderName = "Channel",
        uploaderUrl = "https://channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 30L,
        isLive = false,
        viewCount = 1L,
        uploadedAtMillis = 1L,
        isShortFormContent = true,
        shortDescription = null,
    )
}
