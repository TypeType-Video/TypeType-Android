package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.EndScreenItem
import dev.typetype.android.domain.stream.EndScreenElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndScreenMapperTest {
    private fun item(
        type: String = "video",
        url: String = "https://www.youtube.com/watch?v=x",
        startMs: Long = 590_000,
        endMs: Long = 610_000,
        left: Double = 0.65,
        width: Double = 0.32,
        aspectRatio: Double = 1.77,
    ) = EndScreenItem("id", type, "Title", url, "https://i.ytimg.com/a.jpg", startMs, endMs, left, 0.13, width, aspectRatio)

    @Test
    fun `maps every element type the server sends`() {
        assertEquals(EndScreenElementType.Video, item("video").toDomainEndScreenElement()?.type)
        assertEquals(EndScreenElementType.Playlist, item("playlist").toDomainEndScreenElement()?.type)
        assertEquals(EndScreenElementType.Channel, item("channel").toDomainEndScreenElement()?.type)
        assertEquals(EndScreenElementType.Website, item("website").toDomainEndScreenElement()?.type)
    }

    @Test
    fun `keeps timing and placement`() {
        val element = requireNotNull(item().toDomainEndScreenElement())

        assertEquals(590_000L, element.startMs)
        assertEquals(610_000L, element.endMs)
        assertEquals(0.65f, element.left, 0.0001f)
        assertEquals(0.13f, element.top, 0.0001f)
        assertEquals(0.32f, element.width, 0.0001f)
        assertEquals("https://i.ytimg.com/a.jpg", element.thumbnailUrl)
    }

    @Test
    fun `drops elements it cannot place or open`() {
        assertNull(item(type = "merch").toDomainEndScreenElement())
        assertNull(item(url = "").toDomainEndScreenElement())
        assertNull(item(startMs = 610_000, endMs = 610_000).toDomainEndScreenElement())
        assertNull(item(left = 1.0).toDomainEndScreenElement())
        assertNull(item(left = -0.1).toDomainEndScreenElement())
        assertNull(item(width = 0.0).toDomainEndScreenElement())
        assertNull(item(aspectRatio = 0.0).toDomainEndScreenElement())
        assertNull(item(aspectRatio = Double.NaN).toDomainEndScreenElement())
    }
}
