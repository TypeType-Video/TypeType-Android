package dev.typetype.android.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {

    @Test
    fun `parses server push payload`() {
        val content = """
            {
              "version": 1,
              "eventType": "subscription_new_video",
              "serviceId": 0,
              "serviceName": "YouTube",
              "eventId": "abc123",
              "videoId": "lvlWtH2UGDE",
              "videoUrl": "https://www.youtube.com/watch?v=lvlWtH2UGDE",
              "channelId": "https://www.youtube.com/channel/abc",
              "channelName": "12K UHD WORLD",
              "channelAvatarUrl": "https://example/avatar.png",
              "instanceId": "instance",
              "accountId": "user-1",
              "publishedAt": 1760000000000,
              "title": "A brand new video"
            }
        """.trimIndent()

        val payload = parsePushPayload(content.toByteArray())!!

        assertEquals("subscription_new_video", payload.eventType)
        assertEquals("lvlWtH2UGDE", payload.videoId)
        assertEquals("12K UHD WORLD", payload.channelName)
        assertEquals("A brand new video", payload.title)
    }

    @Test
    fun `ignores unknown fields`() {
        val content = """
            {
              "eventType": "subscription_new_video",
              "serviceId": 0,
              "serviceName": "YouTube",
              "eventId": "abc123",
              "videoId": "v1",
              "videoUrl": "https://example/watch?v=v1",
              "channelId": "c1",
              "channelName": "Channel",
              "channelAvatarUrl": "",
              "instanceId": "instance",
              "accountId": "user-1",
              "publishedAt": 1760000000000,
              "title": "t",
              "futureField": true
            }
        """.trimIndent()

        val payload = parsePushPayload(content.toByteArray())!!

        assertEquals("t", payload.title)
    }

    @Test
    fun `returns null for malformed payload`() {
        assertNull(parsePushPayload("not json".toByteArray()))
    }
}
