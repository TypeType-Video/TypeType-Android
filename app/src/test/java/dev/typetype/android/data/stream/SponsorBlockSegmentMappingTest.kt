package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SponsorBlockSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorBlockSegmentMappingTest {
    @Test
    fun `server millisecond segments keep their timing`() {
        val segment = item(start = 55_649.0, end = 174_610.0)
            .toDomainSponsorBlockSegment(durationSeconds = 600)

        assertEquals(55_649L, segment?.startMs)
        assertEquals(174_610L, segment?.endMs)
    }

    @Test
    fun `legacy second segments are normalized to milliseconds`() {
        val segment = item(start = 10.25, end = 20.75)
            .toDomainSponsorBlockSegment(durationSeconds = 100)

        assertEquals(10_250L, segment?.startMs)
        assertEquals(20_750L, segment?.endMs)
    }

    @Test
    fun `invalid segment is discarded`() {
        assertNull(item(start = 20.0, end = 10.0).toDomainSponsorBlockSegment(100))
        assertNull(item(start = Double.NaN, end = 10.0).toDomainSponsorBlockSegment(100))
    }

    private fun item(start: Double, end: Double) = SponsorBlockSegmentItem(
        startTime = start,
        endTime = end,
        category = "sponsor",
        action = "skip",
    )
}
