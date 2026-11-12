package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@androidx.media3.common.util.UnstableApi
@RunWith(AndroidJUnit4::class)
class YouTubeBufferLoadControlAndroidTest {
    @Test
    fun playerCanBeCreatedWithTheProductionLoadControl() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
            val player = ExoPlayer.Builder(context)
                .setLoadControl(createPlaybackLoadControl(requireNotNull(activityManager)))
                .build()
            player.release()
        }
    }

    @Test
    fun backBufferCallbacksPreserveTheDelegatePolicy() {
        withFixture(youtube = true) {
            assertEquals(15_000_000L, control.getBackBufferDurationUs(playerId))
            assertTrue(control.retainBackBufferFromKeyframe(playerId))
        }
    }

    @Test
    fun selectedTargetExtendsOnlyYouTubeVodAndKeepsFastStartup() {
        withFixture(youtube = true) {
            assertTrue(control.shouldContinueLoading(parameters(40_000_000)))
            assertFalse(control.shouldContinueLoading(parameters(120_000_000)))
            assertTrue(control.shouldStartPlayback(parameters(2_000_000)))
        }
        withFixture(youtube = false) {
            assertFalse(control.shouldContinueLoading(parameters(40_000_000)))
        }
        withFixture(youtube = true, live = true) {
            assertFalse(control.shouldContinueLoading(parameters(40_000_000)))
        }
    }

    @Test
    fun byteLimitStillStopsTheLongBuffer() {
        withFixture(youtube = true) {
            val allocator = control.getAllocator(playerId)
            val allocations = buildList {
                while (allocator.totalBytesAllocated < 2 * 1024 * 1024) add(allocator.allocate())
            }
            try {
                assertFalse(control.shouldContinueLoading(parameters(10_000_000)))
            } finally {
                allocations.forEach(allocator::release)
            }
        }
    }

    private fun withFixture(youtube: Boolean, live: Boolean = false, block: Fixture.() -> Unit) {
        val policy = PlaybackBufferPolicy(targetBufferBytes = 2 * 1024 * 1024)
        val item = MediaItem.Builder().setUri("https://instance.test/video")
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(Bundle().apply {
                if (youtube) putString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY, "request")
                putInt(MergedStreamMediaKeys.EXTRA_YOUTUBE_BUFFER_SECONDS, 120)
                putBoolean(MergedStreamMediaKeys.EXTRA_SABR_IS_LIVE, live)
            }).build()).build()
        val fixture = Fixture(
            YouTubeBufferLoadControl(createPlaybackLoadControl(policy), policy),
            PlayerId("youtube-buffer-test"),
            SinglePeriodTimeline(180_000_000, true, false, false, null, item),
        )
        fixture.control.onPrepared(fixture.playerId)
        fixture.control.onTracksSelected(fixture.parameters(0), TrackGroupArray.EMPTY, emptyArray<ExoTrackSelection>())
        try {
            fixture.block()
        } finally {
            fixture.control.onReleased(fixture.playerId)
        }
    }

    private data class Fixture(
        val control: LoadControl,
        val playerId: PlayerId,
        val timeline: SinglePeriodTimeline,
    ) {
        fun parameters(bufferedUs: Long) = LoadControl.Parameters(
            playerId, timeline, MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)),
            0, bufferedUs, 1f, true, false, C.TIME_UNSET, C.TIME_UNSET,
        )
    }
}
