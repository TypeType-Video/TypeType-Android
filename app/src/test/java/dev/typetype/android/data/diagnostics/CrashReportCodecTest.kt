package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportCodecTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripPreservesOnlyTheRedactedReport() {
        val report = report()

        val encoded = CrashReportCodec.encode(json, report, acknowledged = false)
        val decoded = CrashReportCodec.decode(json, encoded)

        assertEquals(report, decoded?.report)
        assertFalse(decoded?.acknowledged ?: true)
        assertFalse(encoded.contains("message"))
        assertFalse(encoded.contains("email"))
    }

    @Test
    fun malformedOrUnsafeTraceIsRejected() {
        val encoded = CrashReportCodec.encode(json, report(), acknowledged = true)
        val unsafe = encoded.replace(
            "at dev.typetype.android.Player.enterPip:123",
            "https://private.example/?token=secret",
        )

        assertNull(CrashReportCodec.decode(json, unsafe))
        assertTrue(CrashReportCodec.decode(json, encoded)?.acknowledged == true)
    }

    private fun report() = CrashReport(
        occurredAtEpochMillis = 1_700_000_000_000,
        appVersion = "1.2.1",
        appVersionCode = 10_201,
        androidVersion = "10",
        apiLevel = 29,
        deviceManufacturer = "samsung",
        deviceModel = "SM-N960F",
        exceptionType = "java.lang.IllegalStateException",
        fingerprint = "0123456789abcdef",
        stackTrace = listOf(
            "Exception: java.lang.IllegalStateException",
            "at dev.typetype.android.Player.enterPip:123",
        ),
        lastRequest = CrashRequestMetadata("POST", "/sabr/playback/position", "safe-request-id"),
        lastSabrSummary = "session=s-0123456789ab generation=3 state=ready",
    )
}
