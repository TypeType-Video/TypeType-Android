package dev.typetype.android.feature.shorts

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val PAGE_TRANSITION_TIMEOUT_MILLIS = 5_000L

class ShortsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        show(ShortsState(isLoading = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun pagerShowsAPlayableShort() {
        val playedUrl = AtomicReference<String>()
        val video = video("one")

        show(
            state = ShortsState(videos = listOf(video), isLoading = false),
            onPlayVideo = playedUrl::set,
        )

        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Short one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play Short one").performClick()
        assertEquals(video.url, playedUrl.get())
    }

    @Test
    fun emptyFeedHasAnExplicitState() {
        show(ShortsState(isLoading = false))

        composeRule.onNodeWithText("No Shorts are available yet.").assertIsDisplayed()
    }

    @Test
    fun failureKeepsTheRequestIdReviewable() {
        show(
            ShortsState(
                isLoading = false,
                errorMessage = "The instance could not load Shorts",
                errorRequestId = "request-shorts",
            ),
        )

        composeRule.onNodeWithText("The instance could not load Shorts").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-shorts").assertIsDisplayed()
    }

    @Test
    fun embeddedPlaybackCanAdvanceToTheNextShort() {
        val activeUrl = AtomicReference<String>()

        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two")),
                isLoading = false,
            ),
            embeddedPlaybackEnabled = true,
            onActiveVideoChanged = { activeUrl.set(it?.url) },
            embeddedPlayback = { video, onAdvance ->
                Text("Embedded ${video.title}")
                Button(onClick = onAdvance) { Text("Advance") }
            },
        )

        composeRule.waitUntil { activeUrl.get() == "https://video/one" }
        composeRule.onNodeWithText("Embedded Short one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Open Short one in the full player",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Advance").performClick()
        composeRule.waitUntil(timeoutMillis = PAGE_TRANSITION_TIMEOUT_MILLIS) {
            activeUrl.get() == "https://video/two"
        }
        composeRule.onNodeWithText("Embedded Short two").assertIsDisplayed()
    }

    @Test
    fun embeddedPagerDoesNotFlashPlayButtonsOnAdjacentPages() {
        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two")),
                isLoading = false,
            ),
            embeddedPlaybackEnabled = true,
            embeddedPlayback = { video, _ -> Text("Embedded ${video.title}") },
        )

        composeRule.onNodeWithText("Embedded Short one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play Short two").assertDoesNotExist()
    }

    @Test
    fun cancelledSwipeKeepsTheCurrentShortPlaying() {
        val activeUrl = AtomicReference<String>()
        val inactiveEvents = AtomicInteger()
        val playbackDisposals = AtomicInteger()

        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two")),
                isLoading = false,
            ),
            embeddedPlaybackEnabled = true,
            onActiveVideoChanged = {
                activeUrl.set(it?.url)
                if (it == null) inactiveEvents.incrementAndGet()
            },
            embeddedPlayback = { video, _ ->
                DisposableEffect(video.id) {
                    onDispose { playbackDisposals.incrementAndGet() }
                }
                Text("Embedded ${video.title}")
            },
        )

        composeRule.waitUntil { activeUrl.get() == "https://video/one" }
        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput {
            down(center)
            moveTo(center.copy(y = -center.y * 0.5f), 180L)
            moveTo(center, 180L)
            up()
        }
        composeRule.waitForIdle()

        assertEquals("https://video/one", activeUrl.get())
        assertEquals(0, inactiveEvents.get())
        assertEquals(0, playbackDisposals.get())
        composeRule.onNodeWithText("Embedded Short one").assertIsDisplayed()
    }

    @Test
    fun horizontalSwipeLeftOpensTheShortChannel() {
        val openedChannel = AtomicReference<String>()

        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            onOpenChannel = openedChannel::set,
        )

        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput {
            swipe(
                start = center.copy(x = center.x + 180f),
                end = center.copy(x = center.x - 180f),
                durationMillis = 300,
            )
        }

        composeRule.waitUntil { openedChannel.get() == "https://channel" }
    }

    @Test
    fun pagerReportsTwoUpcomingShortsForPlaybackPrefetch() {
        val upcomingUrls = AtomicReference<List<String>>()

        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two"), video("three")),
                isLoading = false,
            ),
            onUpcomingVideosChanged = { videos -> upcomingUrls.set(videos.map(Video::url)) },
        )

        composeRule.waitUntil {
            upcomingUrls.get() == listOf("https://video/two", "https://video/three")
        }
    }

    @Test
    fun changingPageCancelsThePreviousPlaybackPrefetch() {
        val prefetchStarted = AtomicBoolean()
        val prefetchCancelled = AtomicBoolean()

        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two")),
                isLoading = false,
            ),
            embeddedPlaybackEnabled = true,
            onUpcomingVideosChanged = { videos ->
                if (videos.isNotEmpty()) {
                    prefetchStarted.set(true)
                    try {
                        awaitCancellation()
                    } finally {
                        prefetchCancelled.set(true)
                    }
                }
            },
            embeddedPlayback = { _, onAdvance ->
                Button(onClick = onAdvance) { Text("Advance") }
            },
        )

        composeRule.waitUntil { prefetchStarted.get() }
        composeRule.onNodeWithText("Advance").performClick()
        composeRule.waitUntil { prefetchCancelled.get() }
    }

    @Test
    fun activeShortExposesNativeLibraryActions() {
        val selectedAction = AtomicReference<VideoMenuAction>()

        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            embeddedPlaybackEnabled = true,
            onMenuAction = { action, _ -> selectedAction.set(action) },
        )

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertEquals(VideoMenuAction.ToggleFavorite, selectedAction.get())
    }

    @Test
    fun activeShortExposesCommentsAndSubscription() {
        val commentsVideo = AtomicReference<Video>()
        val subscribedVideo = AtomicReference<Video>()

        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            embeddedPlaybackEnabled = true,
            onShowComments = commentsVideo::set,
            onToggleSubscription = subscribedVideo::set,
        )

        composeRule.onNodeWithContentDescription("Comments").performClick()
        composeRule.onNodeWithText("Subscribe").performClick()
        assertEquals("one", commentsVideo.get().id)
        assertEquals("one", subscribedVideo.get().id)
    }

    @Test
    fun activeShortShowsViewsAndLikes() {
        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            statsForVideo = { ShortsVideoStats(viewCount = 1_500, likeCount = 42) },
        )

        composeRule.onNodeWithContentDescription("1.5K views").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("42 likes").assertIsDisplayed()
    }

    @Test
    fun backButtonIsAvailableOverTheShort() {
        val navigatedBack = AtomicBoolean()

        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            onNavigateBack = { navigatedBack.set(true) },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(navigatedBack.get())
    }

    private fun show(
        state: ShortsState,
        onPlayVideo: (String) -> Unit = {},
        onNavigateBack: () -> Unit = {},
        onOpenChannel: (String) -> Unit = {},
        embeddedPlaybackEnabled: Boolean = false,
        onActiveVideoChanged: (Video?) -> Unit = {},
        onUpcomingVideosChanged: suspend (List<Video>) -> Unit = {},
        statsForVideo: (Video) -> ShortsVideoStats = { ShortsVideoStats(it.viewCount, null) },
        embeddedPlayback: @Composable (Video, () -> Unit) -> Unit = { _, _ -> },
        onMenuAction: (VideoMenuAction, Video) -> Unit = { _, _ -> },
        onShowComments: ((Video) -> Unit)? = null,
        onToggleSubscription: (Video) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = state,
                        onNavigateBack = onNavigateBack,
                        onPlayVideo = onPlayVideo,
                        onOpenChannel = onOpenChannel,
                        onRefresh = {},
                        onLoadMore = {},
                        embeddedPlaybackEnabled = embeddedPlaybackEnabled,
                        onActiveVideoChanged = onActiveVideoChanged,
                        onUpcomingVideosChanged = onUpcomingVideosChanged,
                        statsForVideo = statsForVideo,
                        embeddedPlayback = embeddedPlayback,
                        onMenuAction = onMenuAction,
                        onShowComments = onShowComments,
                        onToggleSubscription = onToggleSubscription,
                    )
                }
            }
        }
    }

    private fun video(id: String) = Video(
        id = id,
        url = "https://video/$id",
        title = "Short $id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 30,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = true,
        shortDescription = null,
    )
}
