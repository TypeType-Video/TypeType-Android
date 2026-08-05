package dev.typetype.android.domain.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingVideoRequestTest {

    @Test
    fun requestWaitsUntilPlaybackIsReady() {
        val request = PendingVideoRequest()

        assertNull(request.submit("first"))
        assertEquals("first", request.setReady(true))
        assertNull(request.setReady(true))
    }

    @Test
    fun newestRequestReplacesAnOlderQueuedRequest() {
        val request = PendingVideoRequest()

        request.submit("first")
        request.submit("second")

        assertEquals("second", request.setReady(true))
    }

    @Test
    fun readyRequestIsReturnedImmediately() {
        val request = PendingVideoRequest()
        request.setReady(true)

        assertEquals("video", request.submit("video"))
    }

    @Test
    fun revisionDetectsRequestsArrivingDuringRestore() {
        val request = PendingVideoRequest()
        val restoreRevision = request.currentRevision

        request.submit("video")

        assertFalse(request.isCurrent(restoreRevision))
        assertEquals("video", request.setReady(true))
        assertTrue(request.isCurrent(request.currentRevision))
    }

    @Test
    fun clearDropsPendingRequestAndDisablesImmediateDelivery() {
        val request = PendingVideoRequest()
        request.submit("before sign out")
        request.clear()

        assertNull(request.setReady(true))
        assertEquals("after sign in", request.submit("after sign in"))
    }
}
