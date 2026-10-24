package dev.typetype.android.services.push

import dev.typetype.android.data.account.AccountScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushMessageClassificationTest {
    @Test
    fun `accepts only supported event for registered instance scope`() {
        val result = classifyPushMessage("server-id:account-id", payload(EVENT_TYPE).toByteArray())

        assertTrue(result is PushMessageClassification.Accepted)
        val accepted = result as PushMessageClassification.Accepted
        assertEquals(AccountScope("server-id", "account-id"), accepted.scope)
        assertEquals(EVENT_TYPE, accepted.payload.eventType)
        assertEquals(EVENT_ID, accepted.payload.eventId)
    }

    @Test
    fun `rejects malformed payload without losing instance scope`() {
        val result = classifyPushMessage("server-id:account-id", "not-json".toByteArray())

        assertTrue(result is PushMessageClassification.Rejected)
        val rejected = result as PushMessageClassification.Rejected
        assertEquals(AccountScope("server-id", "account-id"), rejected.scope)
        assertEquals("invalid_payload", rejected.failureCode)
    }

    @Test
    fun `rejects unsupported event types`() {
        val result = classifyPushMessage("server-id:account-id", payload("unknown_event").toByteArray())

        assertTrue(result is PushMessageClassification.Rejected)
        assertEquals("unsupported_event_type", (result as PushMessageClassification.Rejected).failureCode)
    }

    @Test
    fun `rejects payload from a different account`() {
        val result = classifyPushMessage(
            "server-id:account-id",
            payload(EVENT_TYPE, accountId = "other-account").toByteArray(),
        )

        assertTrue(result is PushMessageClassification.Rejected)
        assertEquals("account_mismatch", (result as PushMessageClassification.Rejected).failureCode)
    }

    @Test
    fun `rejects unsupported payload versions`() {
        val result = classifyPushMessage(
            "server-id:account-id",
            payload(EVENT_TYPE, version = 2).toByteArray(),
        )

        assertTrue(result is PushMessageClassification.Rejected)
        assertEquals("unsupported_payload_version", (result as PushMessageClassification.Rejected).failureCode)
    }

    @Test
    fun `rejects unknown instance without assigning another account`() {
        val result = classifyPushMessage("malformed-instance", payload(EVENT_TYPE).toByteArray())

        assertTrue(result is PushMessageClassification.Rejected)
        val rejected = result as PushMessageClassification.Rejected
        assertNull(rejected.scope)
        assertEquals("unknown_instance", rejected.failureCode)
    }

    private fun payload(eventType: String, accountId: String = "account-id", version: Int = 1) = """
        {
          "version": $version,
          "eventType": "$eventType",
          "serviceId": 0,
          "serviceName": "YouTube",
          "eventId": "$EVENT_ID",
          "videoId": "video-id",
          "videoUrl": "https://www.youtube.com/watch?v=video-id",
          "channelId": "channel-id",
          "channelName": "Channel",
          "channelAvatarUrl": "",
          "instanceId": "server-instance",
          "accountId": "$accountId",
          "publishedAt": 1760000000000,
          "title": "New video"
        }
    """.trimIndent()

    private companion object {
        const val EVENT_TYPE = "subscription_new_video"
        const val EVENT_ID = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
    }
}
