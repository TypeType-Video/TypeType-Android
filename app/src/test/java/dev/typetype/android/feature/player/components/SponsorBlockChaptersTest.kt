package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockChaptersTest {
    @Test
    fun `stream chapters always take priority`() {
        val chapters = listOf(Chapter("Introduction", 0L, null))

        val result = playbackChapters(
            streamChapters = chapters,
            policy = policy(showChapters = true, segment(30_000L, SponsorCategory.Sponsor)),
            categoryLabel = { it.key },
        )

        assertEquals(chapters, result)
    }

    @Test
    fun `disabled SponsorBlock chapters stay hidden`() {
        val result = playbackChapters(
            streamChapters = emptyList(),
            policy = policy(showChapters = false, segment(30_000L, SponsorCategory.Sponsor)),
            categoryLabel = { it.key },
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `SponsorBlock chapters are sorted and deduplicated by start`() {
        val result = playbackChapters(
            streamChapters = emptyList(),
            policy = policy(
                showChapters = true,
                segment(40_000L, SponsorCategory.Outro),
                segment(10_000L, SponsorCategory.Intro),
                segment(10_000L, SponsorCategory.Sponsor),
            ),
            categoryLabel = { "label-${it.key}" },
        )

        assertEquals(
            listOf(
                Chapter("label-intro", 10_000L, null),
                Chapter("label-outro", 40_000L, null),
            ),
            result,
        )
    }

    private fun policy(
        showChapters: Boolean,
        vararg segments: SponsorBlockSegment,
    ) = SponsorBlockPlaybackPolicy(
        visibleSegments = segments.toList(),
        showChapters = showChapters,
    )

    private fun segment(startMs: Long, category: SponsorCategory) = SponsorBlockSegment(
        startMs = startMs,
        endMs = startMs + 5_000L,
        category = category,
        action = SponsorAction.Skip,
    )
}
