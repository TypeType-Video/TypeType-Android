package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamRequestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerChannelMetadataCacheTest {
    private val cache = PlayerChannelMetadataCache()
    private val firstScope = StreamRequestScope("server", "first", "https://instance/api/")
    private val secondScope = StreamRequestScope("server", "second", "https://instance/api/")
    private val movedScope = StreamRequestScope("server", "first", "https://moved/api/")

    @Test
    fun `channel metadata is isolated by full request scope`() {
        cache.put(firstScope, "channel", metadata("First"))

        assertEquals("First", cache.get(firstScope, "channel")?.name)
        assertNull(cache.get(secondScope, "channel"))
        assertNull(cache.get(movedScope, "channel"))
    }

    @Test
    fun `missing request scope never reads or writes shared metadata`() {
        cache.put(null, "channel", metadata("Unscoped"))

        assertNull(cache.get(null, "channel"))
        assertNull(cache.get(firstScope, "channel"))
    }

    @Test
    fun `equivalent channel URLs share one scoped entry`() {
        cache.put(firstScope, "http://youtube.com/@channel/?view=1", metadata("Channel"))

        assertEquals("Channel", cache.get(firstScope, "https://youtube.com/@channel")?.name)
    }

    @Test
    fun `oldest channel is evicted from the bounded cache`() {
        repeat(33) { index ->
            cache.put(firstScope, "channel-$index", metadata("Channel $index"))
        }

        assertNull(cache.get(firstScope, "channel-0"))
        assertEquals("Channel 32", cache.get(firstScope, "channel-32")?.name)
    }

    private fun metadata(name: String) = PlayerChannelMetadata(
        name = name,
        avatarUrl = "avatar",
        subscriberCount = 1L,
        verified = false,
    )
}
