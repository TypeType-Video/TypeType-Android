package dev.typetype.android.domain.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class EndScreenElementTest {
    private fun element(
        id: String = "a",
        startMs: Long = 590_000,
        endMs: Long = 610_000,
        left: Float = 0.6f,
        top: Float = 0.1f,
        width: Float = 0.3f,
        aspectRatio: Float = 16f / 9f,
    ) = EndScreenElement(
        id = id,
        type = EndScreenElementType.Video,
        title = "Title",
        url = "https://www.youtube.com/watch?v=x",
        thumbnailUrl = "",
        startMs = startMs,
        endMs = endMs,
        left = left,
        top = top,
        width = width,
        aspectRatio = aspectRatio,
    )

    @Test
    fun `shows an element from its start up to its end`() {
        val elements = listOf(element("early", startMs = 580_000), element("late", startMs = 600_000))

        assertEquals(emptyList<String>(), elements.visibleAt(579_999).map { it.id })
        assertEquals(listOf("early"), elements.visibleAt(580_000).map { it.id })
        assertEquals(listOf("early", "late"), elements.visibleAt(609_999).map { it.id })
        assertEquals(emptyList<String>(), elements.visibleAt(610_000).map { it.id })
    }

    @Test
    fun `keeps the element aspect ratio inside a frame of the same ratio`() {
        val box = element().boxIn(frameAspectRatio = 16f / 9f)

        assertEquals(0.3f, box.width, 0.0001f)
        assertEquals(0.3f, box.height, 0.0001f)
        assertEquals(0.6f, box.left, 0.0001f)
    }

    @Test
    fun `a round element is as tall as it is wide on screen`() {
        val box = element(aspectRatio = 1f, width = 0.15f).boxIn(frameAspectRatio = 16f / 9f)

        assertEquals(0.15f * 16f / 9f, box.height, 0.0001f)
    }

    @Test
    fun `clamps an element that would leave the frame`() {
        val box = element(left = 0.8f, top = 0.9f, width = 0.5f).boxIn(frameAspectRatio = 16f / 9f)

        assertEquals(0.2f, box.width, 0.0001f)
        assertEquals(0.1f, box.height, 0.0001f)
    }
}
