package dev.typetype.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellTransitionTest {
    @Test
    fun alwaysReportsPlaybackEdges() {
        assertTrue(shouldReportPlayerProgress(1f, 0f))
        assertTrue(shouldReportPlayerProgress(0f, 1f))
        assertTrue(shouldReportPlayerProgress(-1f, 2f))
    }

    @Test
    fun skipsSmallIntermediateChanges() {
        assertFalse(shouldReportPlayerProgress(0.20f, 0.24f))
        assertFalse(shouldReportPlayerProgress(0.80f, 0.76f))
    }

    @Test
    fun reportsFullTransitionSteps() {
        assertTrue(shouldReportPlayerProgress(0f, 0.05f))
        assertTrue(shouldReportPlayerProgress(0.95f, 1f))
        assertTrue(shouldReportPlayerProgress(2f, 1f))
    }
}
