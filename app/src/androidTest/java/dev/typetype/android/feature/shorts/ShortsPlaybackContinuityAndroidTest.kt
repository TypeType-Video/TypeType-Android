package dev.typetype.android.feature.shorts

import android.content.ComponentName
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.player.components.ResilientPlayerSurface
import dev.typetype.android.feature.player.state.ResizeMode
import dev.typetype.android.services.PlaybackService
import dev.typetype.android.services.createSyntheticH264Video
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@UnstableApi
class ShortsPlaybackContinuityAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun pagerKeepsOneMediaSessionThroughLongPlaybackAndSeeks() {
        val requestedDurationMs = InstrumentationRegistry.getArguments()
            .getString(DURATION_ARGUMENT)
            ?.toLongOrNull()
            ?.coerceIn(MINIMUM_DURATION_MS, MAXIMUM_DURATION_MS)
            ?: DEFAULT_DURATION_MS
        val video = createSyntheticH264Video(
            context = context,
            durationSeconds = TimeUnit.MILLISECONDS.toSeconds(
                requestedDurationMs + SEEK_HEADROOM_MS,
            ).toInt(),
            frameRate = TEST_FRAME_RATE,
        )
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().get(10, TimeUnit.SECONDS)
        val videos = testVideos(requestedDurationMs)
        val activeId = AtomicReference<String>()

        try {
            showShorts(videos, video, controller, activeId)
            waitForActiveVideo(controller, activeId, videos.first().id)
            exercisePager(controller, activeId, videos, requestedDurationMs)
        } finally {
            instrumentation.runOnMainSync {
                controller.stop()
                controller.clearMediaItems()
                controller.release()
            }
            video.delete()
        }
    }

    private fun showShorts(
        videos: List<Video>,
        file: File,
        controller: MediaController,
        activeId: AtomicReference<String>,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = ShortsState(
                            videos = videos,
                            isLoading = false,
                            autoplayEnabled = true,
                        ),
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRefresh = {},
                        onLoadMore = {},
                        embeddedPlaybackEnabled = true,
                        onActiveVideoChanged = { activeId.set(it?.id) },
                        embeddedPlayback = { current, _ ->
                            LaunchedEffect(current.id) {
                                controller.setMediaItem(
                                    MediaItem.Builder()
                                        .setMediaId(current.id)
                                        .setUri(Uri.fromFile(file))
                                        .setMimeType(MimeTypes.VIDEO_MP4)
                                        .build(),
                                )
                                controller.prepare()
                                controller.play()
                            }
                            ResilientPlayerSurface(
                                player = controller,
                                surfaceKey = current.id,
                                resizeMode = ResizeMode.Crop,
                                showNativeSubtitles = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun exercisePager(
        controller: MediaController,
        activeId: AtomicReference<String>,
        videos: List<Video>,
        requestedDurationMs: Long,
    ) {
        val startedAt = System.nanoTime()
        val durationNs = TimeUnit.MILLISECONDS.toNanos(requestedDurationMs)
        val journey = pageJourney(videos.size)
        val transitionIntervalNs = durationNs / (journey.size + 1)
        var transition = 0
        var currentPage = 0
        var lastPosition = readController(controller) { it.currentPosition }
        var advancementSamples = 0

        while (System.nanoTime() - startedAt < durationNs || transition < journey.size) {
            val elapsedNs = System.nanoTime() - startedAt
            if (transition < journey.size &&
                elapsedNs >= transitionIntervalNs * (transition + 1)
            ) {
                val targetPage = journey[transition]
                composeRule.onNodeWithTag(SHORTS_PAGER_TAG).performTouchInput {
                    if (targetPage > currentPage) swipeUp() else swipeDown()
                }
                val expected = videos[targetPage]
                waitForActiveVideo(controller, activeId, expected.id)
                exerciseForwardAndBackwardSeeks(controller, expected.durationSeconds)
                if (transition == journey.size / 2) exerciseActivityCycle(controller)
                currentPage = targetPage
                transition++
                lastPosition = readController(controller) { it.currentPosition }
            }

            val snapshot = readController(controller) {
                PlaybackSnapshot(
                    positionMs = it.currentPosition,
                    playWhenReady = it.playWhenReady,
                    state = it.playbackState,
                    error = it.playerError,
                )
            }
            assertNull(snapshot.error)
            assertTrue(snapshot.playWhenReady)
            assertTrue(snapshot.state != Player.STATE_IDLE)
            if (snapshot.positionMs > lastPosition + ADVANCEMENT_THRESHOLD_MS) {
                advancementSamples++
            }
            lastPosition = snapshot.positionMs
            Thread.sleep(SAMPLE_INTERVAL_MS)
        }

        assertEquals(journey.size, transition)
        assertTrue(advancementSamples > 0)
    }

    private fun exerciseForwardAndBackwardSeeks(
        controller: MediaController,
        durationSeconds: Long,
    ) {
        val forwardTarget = durationSeconds * 400L
        seekAndVerify(controller, forwardTarget)
        seekAndVerify(controller, forwardTarget / 3L)
        seekAndVerify(controller, forwardTarget)
    }

    private fun waitForActiveVideo(
        controller: MediaController,
        activeId: AtomicReference<String>,
        expectedId: String,
    ) {
        composeRule.waitUntil(timeoutMillis = ACTIVE_VIDEO_TIMEOUT_MS) {
            activeId.get() == expectedId && readController(controller) {
                it.currentMediaItem?.mediaId == expectedId &&
                    it.playerError == null &&
                    it.playWhenReady &&
                    it.playbackState == Player.STATE_READY
            }
        }
    }

    private fun seekAndVerify(controller: MediaController, targetMs: Long) {
        instrumentation.runOnMainSync { controller.seekTo(targetMs) }
        composeRule.waitUntil(timeoutMillis = ACTIVE_VIDEO_TIMEOUT_MS) {
            readController(controller) {
                it.playerError == null &&
                    it.playbackState == Player.STATE_READY &&
                    abs(it.currentPosition - targetMs) <= SEEK_TOLERANCE_MS
            }
        }
    }

    private fun exerciseActivityCycle(controller: MediaController) {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertTrue(readController(controller) { it.playWhenReady })
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        assertTrue(readController(controller) { it.playWhenReady })
    }

    private fun <T> readController(controller: MediaController, block: (MediaController) -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching { block(controller) }) }
        return requireNotNull(result.get()).getOrThrow()
    }

    private fun testVideos(durationMs: Long): List<Video> {
        val count = if (durationMs >= LONG_RUN_THRESHOLD_MS) LONG_VIDEO_COUNT else SMOKE_VIDEO_COUNT
        val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs + SEEK_HEADROOM_MS)
        return List(count) { index ->
            Video(
                id = "short-$index",
                url = "https://video/short-$index",
                title = "Short $index",
                thumbnailUrl = "",
                uploaderName = "Channel $index",
                uploaderUrl = "https://channel/$index",
                uploaderAvatarUrl = "",
                uploaderVerified = false,
                durationSeconds = durationSeconds,
                isLive = false,
                viewCount = index.toLong(),
                uploadedAtMillis = index.toLong(),
                isShortFormContent = true,
                shortDescription = null,
            )
        }
    }

    private fun pageJourney(videoCount: Int): List<Int> = if (videoCount >= LONG_VIDEO_COUNT) {
        listOf(1, 2, 3, 2, 3, 4, 5, 4, 5, 6, 7, 6, 7, 8)
    } else {
        listOf(1, 0, 1, 2)
    }

    private data class PlaybackSnapshot(
        val positionMs: Long,
        val playWhenReady: Boolean,
        val state: Int,
        val error: Throwable?,
    )

    private companion object {
        const val DURATION_ARGUMENT = "shortsContinuityDurationMs"
        const val DEFAULT_DURATION_MS = 10_000L
        const val MINIMUM_DURATION_MS = 5_000L
        const val MAXIMUM_DURATION_MS = 900_000L
        const val SEEK_HEADROOM_MS = 300_000L
        const val LONG_RUN_THRESHOLD_MS = 60_000L
        const val LONG_VIDEO_COUNT = 9
        const val SMOKE_VIDEO_COUNT = 3
        const val TEST_FRAME_RATE = 2
        const val ACTIVE_VIDEO_TIMEOUT_MS = 15_000L
        const val SEEK_TOLERANCE_MS = 2_500L
        const val SAMPLE_INTERVAL_MS = 250L
        const val ADVANCEMENT_THRESHOLD_MS = 100L
    }
}
