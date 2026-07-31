package dev.typetype.android.feature.settings.diagnostics

import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportMarkdownTest {
    @Test
    fun markdownContainsReviewableCrashContextWithoutEmailFlow() {
        val markdown = buildCrashReportMarkdown(report(), "PiP crashed after fullscreen")

        assertTrue(markdown.contains("PiP crashed after fullscreen"))
        assertTrue(markdown.contains("App: 1.2.1 (10201)"))
        assertTrue(markdown.contains("Android: 10 (API 29)"))
        assertTrue(markdown.contains("Device: samsung SM-N960F"))
        assertTrue(markdown.contains("Last request: POST /sabr/playback/position"))
        assertTrue(markdown.contains("Last SABR state: session=s-0123456789ab"))
        assertTrue(markdown.contains("at dev.typetype.android.Player.enterPip:123"))
        assertFalse(markdown.contains("mailto"))
        assertFalse(markdown.contains("email", ignoreCase = true))
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
