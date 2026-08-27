package dev.typetype.android.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerCommentsPagingConfigTest {
    @Test
    fun `comment paging keeps a bounded in-memory cache`() {
        val config = COMMENTS_PAGING_CONFIG

        assertEquals(30, config.pageSize)
        assertEquals(60, config.initialLoadSize)
        assertEquals(10, config.prefetchDistance)
        assertEquals(240, config.maxSize)
        assertFalse(config.enablePlaceholders)
    }
}
