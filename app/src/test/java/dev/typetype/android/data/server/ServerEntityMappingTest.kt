package dev.typetype.android.data.server

import dev.typetype.android.domain.server.PushCapability
import dev.typetype.android.domain.server.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerEntityMappingTest {
    @Test
    fun preservesPushCapabilityAcrossRoomMapping() {
        val server = Server(
            id = "server",
            baseUrl = "https://example.test/api/",
            displayName = "Test",
            addedAt = 1L,
            push = PushCapability(
                enabled = true,
                provider = "unifiedpush",
                eventTypes = listOf("new_video", "channel_live"),
                maxDevicesPerAccount = 3,
            ),
        )

        val restored = ServerEntity.fromDomain(server).toDomain()

        assertTrue(restored.push.enabled)
        assertEquals("unifiedpush", restored.push.provider)
        assertEquals(listOf("new_video", "channel_live"), restored.push.eventTypes)
        assertEquals(3, restored.push.maxDevicesPerAccount)
    }

    @Test
    fun oldRowsRemainPushUnavailableByDefault() {
        val restored = ServerEntity(
            id = "server",
            baseUrl = "https://example.test/api/",
            displayName = "Test",
            addedAt = 1L,
        ).toDomain()

        assertFalse(restored.push.enabled)
        assertEquals("unifiedpush", restored.push.provider)
        assertTrue(restored.push.eventTypes.isEmpty())
    }
}
