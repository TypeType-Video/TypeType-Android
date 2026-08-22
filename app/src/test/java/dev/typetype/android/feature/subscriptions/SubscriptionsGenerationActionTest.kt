package dev.typetype.android.feature.subscriptions

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionsGenerationActionTest {
    @Test
    fun `new server generation replaces the visible feed`() {
        assertEquals(
            SubscriptionsGenerationAction.Replace,
            subscriptionsGenerationAction(4L, 4L, 5L),
        )
    }

    @Test
    fun `matching generation keeps monitoring`() {
        assertEquals(
            SubscriptionsGenerationAction.Continue,
            subscriptionsGenerationAction(4L, 4L, 4L),
        )
    }

    @Test
    fun `stale monitor cannot replace a newer feed`() {
        assertEquals(
            SubscriptionsGenerationAction.Stop,
            subscriptionsGenerationAction(5L, 4L, 6L),
        )
    }
}
