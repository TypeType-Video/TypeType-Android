package dev.typetype.android.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageRetryDelayTest {
    @Test
    fun retriesBackOffWithoutGrowingBeyondThirtyTwoSeconds() {
        assertEquals(1_000L, imageRetryDelayMillis(0))
        assertEquals(2_000L, imageRetryDelayMillis(1))
        assertEquals(32_000L, imageRetryDelayMillis(5))
        assertEquals(32_000L, imageRetryDelayMillis(100))
        assertEquals(32_000L, imageRetryDelayMillis(Int.MAX_VALUE))
    }
}
