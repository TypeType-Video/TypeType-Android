package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrDiagnosticDetailCodecTest {
    @Test
    fun safeDetailRoundTrips() {
        val detail = SabrDiagnosticDetail(
            sessionFingerprint = "s-0123456789ab",
            generation = 3,
            state = SabrDiagnosticDetail.State.Preparing,
            track = SabrDiagnosticDetail.Track.Audio,
            blocker = SabrDiagnosticDetail.Blocker.Discontinuity,
            terminal = SabrDiagnosticDetail.Terminal.SegmentStalled,
            recovery = SabrDiagnosticDetail.Recovery.FreshSession,
            bufferProgress = SabrDiagnosticDetail.BufferProgress.Stalled,
        )

        assertEquals(detail, SabrDiagnosticDetailCodec.decode(SabrDiagnosticDetailCodec.encode(detail)))
    }

    @Test
    fun rawOrUnknownValuesAreRejected() {
        assertNull(SabrDiagnosticDetailCodec.decode("private-session,3,preparing,,,,,"))
        assertNull(SabrDiagnosticDetailCodec.decode("s-0123456789ab,3,private-state,,,,,"))
        assertNull(SabrDiagnosticDetailCodec.decode("s-0123456789ab,-1,preparing,,,,,"))
    }
}
