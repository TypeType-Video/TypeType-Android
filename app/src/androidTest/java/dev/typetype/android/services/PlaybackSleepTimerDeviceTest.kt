package dev.typetype.android.services

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.SilenceMediaSource
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.domain.playback.PlaybackSleepTimerMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackSleepTimerDeviceTest {
    private lateinit var player: ExoPlayer
    private lateinit var timer: PlaybackSleepTimer

    @Before
    fun setUp() {
        onMain {
            player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
            timer = PlaybackSleepTimer()
            timer.attach(player)
        }
    }

    @After
    fun tearDown() {
        onMain {
            timer.detach(player)
            player.release()
        }
    }

    @Test
    fun timedModePausesTheAttachedMedia3Player() {
        onMain {
            player.play()
            timer.start(150L)
        }
        assertTrue(onMain { player.playWhenReady })

        assertTrue(waitUntil { onMain { !player.playWhenReady } })
        assertEquals(PlaybackSleepTimerMode.Off, timer.state.value.mode)
    }

    @Test
    fun endOfVideoModePausesBeforeAdvancingToTheNextItem() {
        val pausedAtEnd = CountDownLatch(1)
        onMain {
            player.addListener(
                object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (
                            !playWhenReady &&
                            reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
                        ) {
                            pausedAtEnd.countDown()
                        }
                    }
                },
            )
            player.setMediaSources(
                listOf(
                    SilenceMediaSource.Factory()
                        .setDurationUs(250_000L)
                        .createMediaSource(),
                    SilenceMediaSource.Factory()
                        .setDurationUs(5_000_000L)
                        .createMediaSource(),
                ),
            )
            player.prepare()
            timer.stopAtEndOfVideo()
            player.play()
        }

        assertTrue(pausedAtEnd.await(5, TimeUnit.SECONDS))
        assertFalse(onMain { player.playWhenReady })
        assertEquals(0, onMain { player.currentMediaItemIndex })
        assertEquals(PlaybackSleepTimerMode.Off, timer.state.value.mode)
    }

    private fun waitUntil(
        timeoutMillis: Long = 3_000L,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (condition()) return true
            Thread.sleep(20L)
        }
        return condition()
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(runCatching(block))
        }
        return result.get().getOrThrow()
    }
}
