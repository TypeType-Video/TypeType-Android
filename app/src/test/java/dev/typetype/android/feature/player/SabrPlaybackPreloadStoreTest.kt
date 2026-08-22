package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SabrPlaybackPreloadStoreTest {
    @Test
    fun `matching preload is reserved once and consumed once`() = runBlocking {
        val store = SabrPlaybackPreloadStore { 0L }
        val first = store.reserve(target())
        val second = store.reserve(target())
        first.result.complete(Result.success(session()))

        assertSame(first.result, second.result)
        assertSame(first.result, store.take(target()))
        assertNull(store.take(target()))
    }

    @Test
    fun `expired preload is not consumed`() {
        var now = 0L
        val store = SabrPlaybackPreloadStore { now }
        store.reserve(target()).result.complete(Result.success(session()))

        now = 120_000_000_001L

        assertNull(store.take(target()))
    }

    private fun target() = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", "https://example.com/api/"),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private fun session() = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://example.com/api/sabr/playback/session/manifest",
        generation = 0,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )
}
