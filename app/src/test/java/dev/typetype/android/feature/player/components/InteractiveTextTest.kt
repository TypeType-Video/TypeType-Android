package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveTextTest {

    @Test
    fun `parses minute and hour timestamps`() {
        val ranges = interactiveTextRanges("Intro 0:42, topic 1:02:03 and long 90:00")
            .filterIsInstance<InteractiveTextRange.Timestamp>()

        assertEquals(listOf(42_000L, 3_723_000L, 5_400_000L), ranges.map { it.positionMillis })
    }

    @Test
    fun `does not parse timestamps inside urls`() {
        val ranges = interactiveTextRanges("Watch https://example.test/watch?t=1:23 then 2:34")

        assertEquals(1, ranges.filterIsInstance<InteractiveTextRange.Url>().size)
        assertEquals(
            listOf(154_000L),
            ranges.filterIsInstance<InteractiveTextRange.Timestamp>().map { it.positionMillis },
        )
    }

    @Test
    fun `normalizes browser links and removes punctuation`() {
        val url = interactiveTextRanges("Docs: www.example.test/help.")
            .filterIsInstance<InteractiveTextRange.Url>()
            .single()

        assertEquals("https://www.example.test/help", url.value)
        assertTrue(url.endExclusive < "Docs: www.example.test/help.".length)
    }
}
