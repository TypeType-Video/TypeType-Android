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

    private fun segment(startMs: Long, endMs: Long) = SponsorBlockSegment(
        startMs = startMs,
        endMs = endMs,
        category = SponsorCategory.Sponsor,
        action = SponsorAction.Skip,
    )
}
