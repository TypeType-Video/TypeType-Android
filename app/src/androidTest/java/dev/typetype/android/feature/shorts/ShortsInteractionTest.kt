package dev.typetype.android.feature.shorts

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ShortsInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun horizontalSwipeLeftOpensTheShortChannelWithHapticFeedback() {
        val openedChannel = AtomicReference<String>()
        val hapticCount = AtomicInteger()
        show(
            onOpenChannel = openedChannel::set,
            hapticFeedback = RecordingHapticFeedback(hapticCount),
        )

        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput {
            swipe(
                start = center.copy(x = center.x + 180f),
                end = center.copy(x = center.x - 180f),
                durationMillis = 300,
            )
        }

        composeRule.waitUntil { openedChannel.get() == "https://channel" }
        assertEquals(1, hapticCount.get())
    }

    @Test
    fun portraitTitleLongPressCopiesWithHapticFeedback() {
        val copiedTitle = AtomicReference<String>()
        val hapticCount = AtomicInteger()
        show(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            onCopyTitle = copiedTitle::set,
            hapticFeedback = RecordingHapticFeedback(hapticCount),
        )

        composeRule.onNodeWithText("Short one").performTouchInput { longClick() }

        assertEquals("Short one", copiedTitle.get())
        assertEquals(1, hapticCount.get())
    }

    @Test
    fun landscapeTitleDoesNotExposeTheCopyGesture() {
        val copiedTitle = AtomicReference<String>()
        show(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            onCopyTitle = copiedTitle::set,
        )

        composeRule.onNodeWithText("Short one").performTouchInput { longClick() }

        assertNull(copiedTitle.get())
    }

    @Test
    fun activePlaybackStartsBeforeUpcomingShortsArePrefetched() {
        val activeUrl = AtomicReference<String>()
        val upcomingUrls = AtomicReference<List<String>>()
        var playbackReady by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
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
                        onActiveVideoChanged = { activeUrl.set(it?.url) },
                        onUpcomingVideosChanged = { videos ->
                            upcomingUrls.set(videos.map(Video::url))
                        },
                    )
                }
            }
        }

        composeRule.waitUntil { activeUrl.get() == "https://video/one" }
        assertNull(upcomingUrls.get())
        composeRule.runOnUiThread { playbackReady = true }
        composeRule.waitUntil {
            upcomingUrls.get() == listOf("https://video/two", "https://video/three")
        }
    }

    private fun show(
        orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        onOpenChannel: (String) -> Unit = {},
        onCopyTitle: (String) -> Unit = {},
        hapticFeedback: HapticFeedback = RecordingHapticFeedback(AtomicInteger()),
    ) {
        val configuration = Configuration().apply { this.orientation = orientation }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAnimatedStatePlayback provides false,
                LocalConfiguration provides configuration,
                LocalHapticFeedback provides hapticFeedback,
            ) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = ShortsState(videos = listOf(video()), isLoading = false),
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenChannel = onOpenChannel,
                        onRefresh = {},
                        onLoadMore = {},
                        onCopyTitle = onCopyTitle,
                    )
                }
            }
        }
    }

    private fun video(id: String = "one") = Video(
        id = id,
        url = "https://video/$id",
        title = "Short $id",
        thumbnailUrl = "",
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

private class RecordingHapticFeedback(
    private val count: AtomicInteger,
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        count.incrementAndGet()
    }
}
