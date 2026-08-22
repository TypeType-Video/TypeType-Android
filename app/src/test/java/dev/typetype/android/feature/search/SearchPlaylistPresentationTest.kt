package dev.typetype.android.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPlaylistPresentationTest {
    @Test
    fun `negative upstream sentinel is not presented as a video count`() {
        assertNull(displayablePlaylistStreamCount(-2))
    }

    @Test
    fun `known playlist count remains visible`() {
        assertEquals(12L, displayablePlaylistStreamCount(12))
    }
}
