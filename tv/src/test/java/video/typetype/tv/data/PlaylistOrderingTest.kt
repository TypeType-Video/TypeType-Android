package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

public class PlaylistOrderingTest {
    @Test
    public fun movesTheSelectedVideoByOnePosition(): Unit {
        assertEquals(listOf("two", "one", "three"), reorderedVideoIds(listOf("one", "two", "three"), "one", 1))
        assertEquals(listOf("one", "three", "two"), reorderedVideoIds(listOf("one", "two", "three"), "three", -1))
    }

    @Test
    public fun rejectsUnknownAndBoundaryMoves(): Unit {
        assertNull(reorderedVideoIds(listOf("one", "two"), "missing", 1))
        assertNull(reorderedVideoIds(listOf("one", "two"), "one", -1))
        assertNull(reorderedVideoIds(listOf("one", "two"), "two", 1))
    }
}
