package dev.typetype.android.feature.settings.diagnostics

import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {
    @Test
    fun reportContainsOnlyRedactedSabrMetadata() {
        val entry = DiagnosticEntry(
            timestampEpochMillis = 1_700_000_000_000,
            method = "POST",
            route = "/sabr/playback/prefetch",
            statusCode = 202,
            durationMillis = 24,
            requestId = "safe-request-id",
            sabr = SabrDiagnosticDetail(
                sessionFingerprint = "s-0123456789ab",
                generation = 2,
                state = SabrDiagnosticDetail.State.Preparing,
                track = SabrDiagnosticDetail.Track.Video,
                blocker = SabrDiagnosticDetail.Blocker.SegmentPending,
                terminal = null,
                recovery = null,
                bufferProgress = SabrDiagnosticDetail.BufferProgress.Stalled,
            ),
        )

        val report = buildDiagnosticReport(listOf(entry))

        assertTrue(report.contains("SABR session=s-0123456789ab"))
        assertTrue(report.contains("blocker=segment-pending"))
        assertTrue(report.contains("buffer=stalled"))
        assertFalse(report.contains("video:137:123"))
        assertFalse(report.contains("private-session"))
    }
}
