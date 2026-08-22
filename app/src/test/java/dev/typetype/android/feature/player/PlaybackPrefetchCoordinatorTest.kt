package dev.typetype.android.feature.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackPrefetchCoordinatorTest {
    @Test
    fun activationWaitsForOneSharedPrefetch() = runBlocking {
        val releasePrefetch = CompletableDeferred<Unit>()
        var requests = 0
        val coordinator = PlaybackPrefetchCoordinator(this) {
            requests += 1
            releasePrefetch.await()
        }

        coordinator.schedule("video")
        coordinator.schedule("video")
        yield()
        val activation = async { coordinator.await("video") }
        yield()

        assertEquals(1, requests)
        assertFalse(activation.isCompleted)
        releasePrefetch.complete(Unit)
        activation.await()
        assertEquals(1, requests)
    }
}
