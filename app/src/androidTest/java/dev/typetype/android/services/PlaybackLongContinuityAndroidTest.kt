package dev.typetype.android.services

import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackLongContinuityAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun sharedMediaSessionSurvivesLongPlaybackAndBidirectionalSeeks() {
        val requestedDurationMs = InstrumentationRegistry.getArguments()
            .getString(DURATION_ARGUMENT)
            ?.toLongOrNull()
            ?.coerceIn(MINIMUM_DURATION_MS, MAXIMUM_DURATION_MS)
            ?: DEFAULT_DURATION_MS
        val mediaDurationMs = requestedDurationMs + SEEK_HEADROOM_MS
        val video = createSyntheticH264Video(
            context = context,
            durationSeconds = TimeUnit.MILLISECONDS.toSeconds(mediaDurationMs).toInt(),
            frameRate = LONG_TEST_FRAME_RATE,
        )
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().get(10, TimeUnit.SECONDS)

        try {
            play(controller, video)
            assertTrue(
                "Local H.264 playback did not become ready",
                waitForController(controller) {
                    it.playerError == null &&
                        it.playWhenReady &&
                        it.playbackState == Player.STATE_READY
                },
            )
            exerciseContinuity(controller, requestedDurationMs, mediaDurationMs)
        } finally {
            instrumentation.runOnMainSync {
                controller.stop()
                controller.clearMediaItems()
                controller.release()
            }
            video.delete()
        }
    }

    private fun play(controller: MediaController, video: java.io.File) {
        instrumentation.runOnMainSync {
            controller.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(video))
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .build(),
            )
            controller.prepare()
            controller.play()
        }
    }

    private fun exerciseContinuity(
        controller: MediaController,
        requestedDurationMs: Long,
        mediaDurationMs: Long,
    ) {
        val targets = seekTargets(mediaDurationMs, requestedDurationMs)
        val startedAt = System.nanoTime()
        val durationNs = TimeUnit.MILLISECONDS.toNanos(requestedDurationMs)
        val seekIntervalNs = durationNs / (targets.size + 1L)
        var nextSeek = 0
        var lastPosition = readController(controller) { it.currentPosition }
        var advancingSamples = 0

        while (System.nanoTime() - startedAt < durationNs) {
            val elapsedNs = System.nanoTime() - startedAt
            if (nextSeek < targets.size && elapsedNs >= seekIntervalNs * (nextSeek + 1L)) {
                val target = targets[nextSeek++]
                instrumentation.runOnMainSync { controller.seekTo(target) }
                assertTrue(
                    "Playback did not recover after seeking to $target ms",
                    waitForController(controller) {
                        it.playerError == null &&
                            it.playWhenReady &&
                            it.playbackState == Player.STATE_READY &&
                            abs(it.currentPosition - target) <= SEEK_TOLERANCE_MS
                    },
                )
                lastPosition = readController(controller) { it.currentPosition }
            }

            val snapshot = readController(controller) {
                PlayerSnapshot(
                    positionMs = it.currentPosition,
                    playWhenReady = it.playWhenReady,
                    playbackState = it.playbackState,
                    error = it.playerError,
                )
            }
            assertNull("Playback failed during continuity run", snapshot.error)
            assertTrue(snapshot.playWhenReady)
            assertTrue(snapshot.playbackState != Player.STATE_IDLE)
            if (snapshot.positionMs > lastPosition + ADVANCEMENT_THRESHOLD_MS) {
                advancingSamples++
            }
            lastPosition = snapshot.positionMs
            Thread.sleep(SAMPLE_INTERVAL_MS)
        }

        assertTrue("Playback never advanced between seeks", advancingSamples > 0)
        assertTrue("Not every scheduled seek ran", nextSeek == targets.size)
    }

    private fun seekTargets(mediaDurationMs: Long, requestedDurationMs: Long): List<Long> {
        val fractions = if (requestedDurationMs >= LONG_RUN_THRESHOLD_MS) {
            LONG_SEEK_FRACTIONS
        } else {
            SMOKE_SEEK_FRACTIONS
        }
        return fractions.map { fraction -> (mediaDurationMs * fraction).toLong() }
    }

    private fun <T> readController(
        controller: MediaController,
        block: (MediaController) -> T,
    ): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching { block(controller) }) }
        return requireNotNull(result.get()).getOrThrow()
    }

    private fun waitForController(
        controller: MediaController,
        condition: (MediaController) -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline) {
            if (readController(controller, condition)) return true
            Thread.sleep(50)
        }
        return readController(controller, condition)
    }

    private data class PlayerSnapshot(
        val positionMs: Long,
        val playWhenReady: Boolean,
        val playbackState: Int,
        val error: Throwable?,
    )

    private companion object {
        const val DURATION_ARGUMENT = "playbackContinuityDurationMs"
        const val DEFAULT_DURATION_MS = 5_000L
        const val MINIMUM_DURATION_MS = 5_000L
        const val MAXIMUM_DURATION_MS = 900_000L
        const val SEEK_HEADROOM_MS = 300_000L
        const val LONG_RUN_THRESHOLD_MS = 60_000L
        const val SEEK_TOLERANCE_MS = 2_500L
        const val SAMPLE_INTERVAL_MS = 250L
        const val ADVANCEMENT_THRESHOLD_MS = 100L
        const val LONG_TEST_FRAME_RATE = 2
        val SMOKE_SEEK_FRACTIONS = listOf(0.60, 0.15)
        val LONG_SEEK_FRACTIONS = listOf(0.65, 0.10, 0.80, 0.35, 0.70, 0.20, 0.55, 0.25)
    }
}
