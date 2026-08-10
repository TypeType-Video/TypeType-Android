package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStreamLoaderTest {
    @Test
    fun `playback becomes ready before slow metadata completes`() = runBlocking {
        val metadataGate = CompletableDeferred<Unit>()

        val first = withTimeout(1_000) {
            loadProgressiveStream(
                loadPlayback = { Result.success(stream("Bootstrap")) },
                loadMetadata = {
                    metadataGate.await()
                    Result.success(stream("Detailed"))
                },
                loadProgress = { 0L },
            ).first()
        }

        assertTrue(first is PlayerStreamUpdate.PlaybackReady)
        assertFalse(metadataGate.isCompleted)
    }

    @Test
    fun `server metadata starts while playback bootstrap is running`() = runBlocking {
        val playbackGate = CompletableDeferred<Unit>()
        val metadataStarted = CompletableDeferred<Unit>()
        val updates = Channel<PlayerStreamUpdate>(Channel.UNLIMITED)
        val collection = launch {
            loadProgressiveStream(
                loadPlayback = {
                    playbackGate.await()
                    Result.success(stream("Bootstrap"))
                },
                loadMetadata = {
                    metadataStarted.complete(Unit)
                    Result.success(stream("Detailed"))
                },
                loadProgress = { 0L },
            ).collect(updates::send)
        }

        withTimeout(1_000) { metadataStarted.await() }
        assertTrue(metadataStarted.isCompleted)
        playbackGate.complete(Unit)
        assertTrue(withTimeout(1_000) { updates.receive() } is PlayerStreamUpdate.PlaybackReady)
        assertTrue(withTimeout(1_000) { updates.receive() } is PlayerStreamUpdate.MetadataEnriched)
        collection.cancel()
    }

    @Test
    fun `metadata enrichment follows playback without changing resume`() = runBlocking {
        val updates = loadProgressiveStream(
            loadPlayback = { Result.success(stream("Bootstrap")) },
            loadMetadata = { Result.success(stream("Detailed")) },
            loadProgress = { 25_000L },
        ).take(2).toList()

        val ready = updates[0] as PlayerStreamUpdate.PlaybackReady
        val enriched = updates[1] as PlayerStreamUpdate.MetadataEnriched
        assertEquals(25_000L, ready.loaded.resumeAtMillis)
        assertEquals("Bootstrap", ready.loaded.stream.title)
        assertEquals("Detailed", enriched.stream.title)
    }

    @Test
    fun `channel metadata enriches subscribers after playback is ready`() = runBlocking {
        val updates = loadProgressiveStream(
            loadPlayback = { Result.success(stream("Bootstrap", uploaderUrl = "channel")) },
            loadMetadata = { null },
            loadChannelMetadata = {
                Result.success(it.copy(uploaderSubscriberCount = 1_530_000L))
            },
            loadProgress = { 0L },
        ).take(2).toList()

        val ready = updates[0] as PlayerStreamUpdate.PlaybackReady
        val enriched = updates[1] as PlayerStreamUpdate.MetadataEnriched
        assertEquals(-1L, ready.loaded.stream.uploaderSubscriberCount)
        assertEquals(1_530_000L, enriched.stream.uploaderSubscriberCount)
    }

    @Test
    fun `complete stream metadata avoids redundant channel lookup`() = runBlocking {
        val channelRequests = AtomicInteger()
        val updates = loadProgressiveStream(
            loadPlayback = { Result.success(stream("Bootstrap", uploaderUrl = "channel")) },
            loadMetadata = {
                Result.success(
                    stream(
                        title = "Detailed",
                        uploaderUrl = "channel",
                        subscriberCount = 42L,
                    ),
                )
            },
            loadChannelMetadata = {
                channelRequests.incrementAndGet()
                Result.success(it)
            },
            loadProgress = { 0L },
        ).take(2).toList()

        assertEquals("Detailed", (updates[1] as PlayerStreamUpdate.MetadataEnriched).stream.title)
        assertEquals(0, channelRequests.get())
    }

    @Test
    fun `prefetched metadata with the same account scope avoids another request`() = runBlocking {
        val metadataRequests = AtomicInteger()
        val updates = loadProgressiveStream(
            loadPlayback = { Result.success(stream("Bootstrap")) },
            loadMetadata = {
                metadataRequests.incrementAndGet()
                Result.success(stream("Unexpected"))
            },
            loadProgress = { 0L },
            prefetchedMetadata = stream("Prefetched"),
        ).take(2).toList()

        val enriched = updates[1] as PlayerStreamUpdate.MetadataEnriched
        assertEquals("Prefetched", enriched.stream.title)
        assertEquals(0, metadataRequests.get())
    }

    @Test
    fun `prefetched metadata from another account is reloaded`() = runBlocking {
        val metadataRequests = AtomicInteger()
        val updates = loadProgressiveStream(
            loadPlayback = { Result.success(stream("Bootstrap")) },
            loadMetadata = {
                metadataRequests.incrementAndGet()
                Result.success(stream("Fresh"))
            },
            loadProgress = { 0L },
            prefetchedMetadata = stream("Wrong account", accountId = "other"),
        ).take(2).toList()

        val enriched = updates[1] as PlayerStreamUpdate.MetadataEnriched
        assertEquals("Fresh", enriched.stream.title)
        assertEquals(1, metadataRequests.get())
    }

    private fun stream(
        title: String,
        accountId: String = "account",
        uploaderUrl: String = "",
        subscriberCount: Long = -1L,
    ) = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = "video",
        title = title,
        uploaderName = "Channel",
        uploaderAvatarUrl = "",
        uploaderUrl = uploaderUrl,
        uploaderSubscriberCount = subscriberCount,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 120L,
        viewCount = -1L,
        likeCount = -1L,
        dislikeCount = -1L,
        uploadedAtMillis = -1L,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        requestScope = StreamRequestScope("server", accountId, "https://instance.example/api/"),
        startPositionMillis = 0L,
    )
}
