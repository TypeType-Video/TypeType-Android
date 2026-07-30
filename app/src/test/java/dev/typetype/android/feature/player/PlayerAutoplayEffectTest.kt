package dev.typetype.android.feature.player

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueAutoplayCountdown
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.playback.PlaybackRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerAutoplayEffectTest {
    @Test
    fun `queue target wins over a related recommendation`() {
        val target = queueAutoplayCountdownState(queue(), {}, {}, {})?.target

        assertEquals("queue-next", target?.videoUrl)
        assertEquals("Queued video", target?.title)
    }

    @Test
    fun `related target is used outside a queue`() {
        val target = selectAutoplayTarget(PlaybackQueueState(), relatedVideo())

        assertEquals("related", target?.videoUrl)
    }

    @Test
    fun `active queue without a next item does not fall through to related`() {
        val target = selectAutoplayTarget(
            PlaybackQueueState(
                entries = listOf(queueEntry("current", "Current video")),
                currentIndex = 0,
            ),
            relatedVideo(),
        )

        assertNull(target)
    }

    @Test
    fun `queue countdown preserves timing and controls`() {
        var played = false
        var cancelled = false
        var paused = false
        val state = queueAutoplayCountdownState(
            playbackQueue = queue(),
            onAdvanceQueue = { played = true },
            onCancel = { cancelled = true },
            onTogglePause = { paused = true },
        )

        assertEquals(5, state?.remainingSeconds)
        assertEquals(0.5f, state?.progress)
        state?.playNow?.invoke()
        state?.cancel?.invoke()
        state?.togglePause?.invoke()
        assertEquals(true, played)
        assertEquals(true, cancelled)
        assertEquals(true, paused)
    }

    @Test
    fun `repeat all points the countdown back to the first item`() {
        val playbackQueue = PlaybackQueueState(
            entries = listOf(
                queueEntry("first", "First video"),
                queueEntry("last", "Last video"),
            ),
            currentIndex = 1,
            repeatMode = PlaybackRepeatMode.All,
            autoplayCountdown = PlaybackQueueAutoplayCountdown(
                targetVideoUrl = "first",
                totalMillis = 10_000,
                remainingMillis = 10_000,
                paused = false,
            ),
        )

        val target = queueAutoplayCountdownState(playbackQueue, {}, {}, {})?.target

        assertEquals("first", target?.videoUrl)
    }

    private fun queue() = PlaybackQueueState(
        entries = listOf(
            queueEntry("current", "Current video"),
            queueEntry("queue-next", "Queued video"),
        ),
        currentIndex = 0,
        autoplayCountdown = PlaybackQueueAutoplayCountdown(
            targetVideoUrl = "queue-next",
            totalMillis = 10_000,
            remainingMillis = 5_000,
            paused = false,
        ),
    )

    private fun queueEntry(url: String, title: String) = PlaybackQueueEntry(
        videoUrl = url,
        title = title,
        thumbnailUrl = "https://example.com/$url.jpg",
        durationSeconds = 120,
        channelName = "Queue channel",
    )

    private fun relatedVideo() = Video(
        id = "related",
        url = "related",
        title = "Related video",
        thumbnailUrl = "https://example.com/related.jpg",
        uploaderName = "Related channel",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 120,
        isLive = false,
        viewCount = 0,
        uploadedAtMillis = 0,
        isShortFormContent = false,
        shortDescription = null,
    )
}
