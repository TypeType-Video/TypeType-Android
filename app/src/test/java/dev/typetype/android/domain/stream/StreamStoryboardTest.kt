package dev.typetype.android.domain.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamStoryboardTest {
    private val storyboard = StreamStoryboard(
        urls = listOf("page-0", "page-1"),
        frameWidth = 160,
        frameHeight = 90,
        totalCount = 100,
        durationPerFrameMillis = 5_000L,
        framesPerPageX = 5,
        framesPerPageY = 5,
    )

    @Test
    fun selectsFrameInsideFirstStoryboardPage() {
        val frame = storyboard.frameAt(57_000L)

        assertEquals("page-0", frame?.url)
        assertEquals(160, frame?.x)
        assertEquals(180, frame?.y)
        assertEquals(160, frame?.width)
        assertEquals(90, frame?.height)
    }

    @Test
    fun selectsPageAndCoordinatesAcrossStoryboardPages() {
        val frame = storyboard.frameAt(150_000L)

        assertEquals("page-1", frame?.url)
        assertEquals(0, frame?.x)
        assertEquals(90, frame?.y)
    }

    @Test
    fun clampsToLastFrameAndRejectsIncompleteStoryboards() {
        assertEquals(640, storyboard.frameAt(Long.MAX_VALUE / 2L)?.x)

        assertNull(storyboard.copy(totalCount = 0).frameAt(0L))
        assertNull(storyboard.copy(urls = emptyList()).frameAt(0L))
    }
}
