package dev.typetype.android.services.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SubscriptionNotificationIdTest {
    @Test
    fun `id follows the video id`() {
        assertEquals(
            "TIcVuMOK78I".hashCode(),
            subscriptionNotificationId("TIcVuMOK78I", "https://www.youtube.com/watch?v=TIcVuMOK78I", "event"),
        )
    }

    @Test
    fun `id falls back to the video url when the id is blank`() {
        val url = "https://www.youtube.com/watch?v=TIcVuMOK78I"
        assertEquals(url.hashCode(), subscriptionNotificationId("  ", url, "event"))
    }

    @Test
    fun `id falls back to the event key when both video fields are blank`() {
        assertEquals("event".hashCode(), subscriptionNotificationId("", "  ", "event"))
    }

    @Test
    fun `push and local notifications share the id of the same video`() {
        val pushId = subscriptionNotificationId("abc123", "https://example.test/watch?v=abc123", "event")
        val localId = subscriptionNotificationId("abc123", "https://example.test/watch?v=abc123", "abc123")
        assertEquals(pushId, localId)
        assertNotEquals(pushId, subscriptionNotificationId("other", "https://example.test/watch?v=other", "event"))
    }
}
