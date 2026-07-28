package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class SponsorBlockSkipperTest {
    @Test
    fun `ordinary segment skips to its end`() {
        assertEquals(20_000L, sponsorBlockSkipTargetMs(segment(10_000L, 20_000L), 60_000L))
    }

    @Test
    fun `terminal segment leaves a playable tail`() {
        assertEquals(
            59_792L,
            sponsorBlockSkipTargetMs(segment(50_000L, 59_991L), 60_000L),
        )
    }

    @Test
    fun `terminal segment beyond duration stays below duration`() {
        assertEquals(
            59_800L,
            sponsorBlockSkipTargetMs(segment(50_000L, 60_500L), 60_000L),
        )
    }

    @Test
    fun `unknown duration preserves the segment end`() {
        assertEquals(20_000L, sponsorBlockSkipTargetMs(segment(10_000L, 20_000L), -1L))
    }

    @Test
    fun `tracker skips when playback crosses a segment start`() {
        val tracker = SponsorBlockSkipTracker()
        val sponsor = segment(10_000L, 20_000L)

        assertEquals(null, tracker.next(9_900L, true, listOf(sponsor)))
        assertEquals(sponsor, tracker.next(10_100L, true, listOf(sponsor)))
    }

    @Test
    fun `tracker does not repeat a pending skip`() {
        val tracker = SponsorBlockSkipTracker()
        val sponsor = segment(10_000L, 20_000L)

        tracker.next(9_900L, true, listOf(sponsor))
        assertEquals(sponsor, tracker.next(10_100L, true, listOf(sponsor)))
        assertEquals(null, tracker.next(10_300L, false, listOf(sponsor)))
        assertEquals(null, tracker.next(10_600L, false, listOf(sponsor)))
    }

    @Test
    fun `tracker can skip the segment again after playback returns before it`() {
        val tracker = SponsorBlockSkipTracker()
        val sponsor = segment(10_000L, 20_000L)

        tracker.next(9_900L, true, listOf(sponsor))
        tracker.next(10_100L, true, listOf(sponsor))
        tracker.next(20_000L, true, listOf(sponsor))
        tracker.next(9_000L, true, listOf(sponsor))

        assertEquals(sponsor, tracker.next(10_100L, true, listOf(sponsor)))
    }

    private fun segment(startMs: Long, endMs: Long) = SponsorBlockSegment(
        startMs = startMs,
        endMs = endMs,
        category = SponsorCategory.Sponsor,
        action = SponsorAction.Skip,
    )
}
