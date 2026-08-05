package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.comments.BulletComment
import dev.typetype.android.domain.comments.BulletCommentPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuPresentationTest {
    @Test
    fun seekUsesTheCurrentPlaybackPosition() {
        val comments = listOf(comment(atMillis = 1_000), comment(atMillis = 9_000))

        assertEquals("first", presentBulletComments(comments, 4_000, 1f, 8).single().comment.text)
        assertEquals("second", presentBulletComments(comments, 10_000, 1f, 8).single().comment.text)
    }

    @Test
    fun speedChangesRegularLifetimeWithoutChangingStaticLifetime() {
        val regular = comment(atMillis = 0)
        val top = comment(atMillis = 0, position = BulletCommentPosition.Top)

        assertTrue(presentBulletComments(listOf(regular), 4_000, 2f, 8).isEmpty())
        assertEquals(1, presentBulletComments(listOf(top), 2_500, 2f, 8).size)
    }

    @Test
    fun filtersBottomCommentsAndBoundsBursts() {
        val comments = List(40) { comment(text = "$it", atMillis = 0) } +
            comment(text = "bottom", atMillis = 0, position = BulletCommentPosition.Bottom)

        val visible = presentBulletComments(comments, 100, 1f, 8)

        assertEquals(24, visible.size)
        assertTrue(visible.none { it.comment.text == "bottom" })
        assertTrue(visible.all { it.lane in 0..7 })
    }

    @Test
    fun serverLimitPayloadOnlyPresentsTheActiveWindow() {
        val comments = List(20_000) { index ->
            comment(text = "$index", atMillis = index * 1_000L)
        }

        val visible = presentBulletComments(comments, 19_999_000L, 1f, 8)

        assertEquals(
            listOf("19994", "19995", "19996", "19997", "19998", "19999"),
            visible.map { it.comment.text },
        )
    }

    private fun comment(
        atMillis: Long,
        text: String = if (atMillis < 5_000) "first" else "second",
        position: BulletCommentPosition = BulletCommentPosition.Regular,
    ) = BulletComment(
        text = text,
        rgbColor = 0xFFFFFF,
        position = position,
        relativeFontSize = 1f,
        presentationTimeMillis = atMillis,
        isLive = false,
    )
}
