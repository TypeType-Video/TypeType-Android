package dev.typetype.android.feature.search

import dev.typetype.android.core.ui.util.knownCountOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPlaylistPresentationTest {
    @Test
    fun `negative upstream sentinel is not presented as a video count`() {
        assertNull((-2L).knownCountOrNull())
    }

    @Test
    fun `known playlist count remains visible`() {
        assertEquals(12L, 12L.knownCountOrNull())
    }

    @Test
    fun `zero remains a known count`() {
        assertEquals(0L, 0L.knownCountOrNull())
    }
}
