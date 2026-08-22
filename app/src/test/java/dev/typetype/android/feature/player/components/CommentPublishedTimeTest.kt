package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentPublishedTimeTest {
    @Test
    fun `technical timestamps are not shown as comment metadata`() {
        assertEquals("", normalizeCommentPublishedTime("2026-08-22T09:30:00Z"))
    }

    @Test
    fun `timezone names are removed from human labels`() {
        assertEquals("2 hours ago", normalizeCommentPublishedTime("2 hours ago (Zulu)"))
        assertEquals("3 minutes ago", normalizeCommentPublishedTime("3 minutes ago UTC"))
    }
}
