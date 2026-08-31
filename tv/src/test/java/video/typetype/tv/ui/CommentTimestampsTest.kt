package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

public class CommentTimestampsTest {
    @Test
    public fun parsesShortAndLongTimestamps(): Unit {
        val timestamps = "Intro 0:42, chapter 1:02:03".commentTimestamps()

        assertEquals(
            listOf(
                CommentTimestamp("0:42", 42),
                CommentTimestamp("1:02:03", 3_723),
            ),
            timestamps,
        )
    }

    @Test
    public fun rejectsInvalidAndEmbeddedTimestamps(): Unit {
        val timestamps = "x12:34 invalid 12:75 1:02:60 2:03 valid".commentTimestamps()

        assertEquals(listOf(CommentTimestamp("2:03", 123)), timestamps)
    }

    @Test
    public fun deduplicatesAndBoundsTimestampActions(): Unit {
        val text = (1..12).joinToString(" ") { "${it}:00" } + " 1:00"

        val timestamps = text.commentTimestamps()

        assertEquals(8, timestamps.size)
        assertEquals(60L, timestamps.first().seconds)
    }
}
