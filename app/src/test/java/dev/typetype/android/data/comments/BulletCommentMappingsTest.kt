package dev.typetype.android.data.comments

import dev.typetype.android.data.network.dto.BulletCommentItem
import dev.typetype.android.domain.comments.BulletCommentPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BulletCommentMappingsTest {

    @Test
    fun mapsAndBoundsTrustedPresentationFields() {
        val comment = item(
            text = "  Hello  ",
            argbColor = 0x0012ABEF,
            position = "regular",
            relativeFontSize = 8.0,
        ).toDomain()

        requireNotNull(comment)
        assertEquals("Hello", comment.text)
        assertEquals(0x0012ABEF, comment.rgbColor)
        assertEquals(BulletCommentPosition.Regular, comment.position)
        assertEquals(2f, comment.relativeFontSize)
        assertEquals(4_200L, comment.presentationTimeMillis)
    }

    @Test
    fun rejectsUnknownEmptyAndNegativeEntries() {
        assertNull(item(position = "UNKNOWN").toDomain())
        assertNull(item(text = "   ").toDomain())
        assertNull(item(durationMs = -1L).toDomain())
    }

    private fun item(
        text: String = "Comment",
        argbColor: Int = -1,
        position: String = "TOP",
        relativeFontSize: Double = 1.0,
        durationMs: Long = 4_200L,
    ) = BulletCommentItem(
        text = text,
        argbColor = argbColor,
        position = position,
        relativeFontSize = relativeFontSize,
        durationMs = durationMs,
        isLive = false,
    )
}
