package dev.typetype.android.services

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressHeartbeatTest {
    @Test
    fun `reports once per interval while playing`() = runBlocking {
        val ticks = Channel<Unit>()
        var reports = 0
        val heartbeat = PlaybackProgressHeartbeat(this, INTERVAL, { ticks.receive() }) { reports++ }

        heartbeat.onPlayingChanged(true)
        ticks.send(Unit)
        ticks.send(Unit)
        yield()

        assertEquals(2, reports)
        heartbeat.stop()
    }

    @Test
    fun `sends nothing while paused`() = runBlocking {
        val ticks = Channel<Unit>()
        var reports = 0
        val heartbeat = PlaybackProgressHeartbeat(this, INTERVAL, { ticks.receive() }) { reports++ }

        heartbeat.onPlayingChanged(true)
        ticks.send(Unit)
        yield()
        heartbeat.onPlayingChanged(false)
        yield()

        assertTrue(ticks.trySend(Unit).isFailure)
        assertEquals(1, reports)
    }

    @Test
    fun `sends nothing before playback starts`() = runBlocking {
        val ticks = Channel<Unit>()
        var reports = 0
        PlaybackProgressHeartbeat(this, INTERVAL, { ticks.receive() }) { reports++ }
        yield()

        assertTrue(ticks.trySend(Unit).isFailure)
        assertEquals(0, reports)
    }

    @Test
    fun `resumes reporting after a pause`() = runBlocking {
        val ticks = Channel<Unit>()
        var reports = 0
        val heartbeat = PlaybackProgressHeartbeat(this, INTERVAL, { ticks.receive() }) { reports++ }

        heartbeat.onPlayingChanged(true)
        heartbeat.onPlayingChanged(false)
        heartbeat.onPlayingChanged(true)
        ticks.send(Unit)
        yield()

        assertEquals(1, reports)
        heartbeat.stop()
    }

    private companion object {
        const val INTERVAL = 15_000L
    }
}
