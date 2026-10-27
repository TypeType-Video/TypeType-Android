package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportSanitizerTest {
    @Test
    fun reportKeepsStaticFramesWithoutExceptionMessagesOrSourcePaths() {
        val secret = "https://private.example/watch?v=secret-token"
        val cause = IllegalArgumentException("cookie=$secret").withFrames()
        val failure = IllegalStateException("request failed for $secret", cause).withFrames()

        val report = CrashReportSanitizer.create(
            throwable = failure,
            environment = environment(),
            diagnostics = context(),
            timestampEpochMillis = 1_700_000_000_000,
        )
        val serializedView = report.toString()

        assertEquals("java.lang.IllegalStateException", report.exceptionType)
        assertTrue(report.stackTrace.any { it == "at dev.typetype.android.Player.enterPip:123" })
        assertTrue(report.stackTrace.any { it == "at android.app.Activity.enterPictureInPictureMode:8071" })
        assertTrue(report.stackTrace.any { it == "Caused by: java.lang.IllegalArgumentException" })
        assertFalse(serializedView.contains(secret))
        assertFalse(serializedView.contains("cookie="))
        assertFalse(serializedView.contains("/home/builder/Player.kt"))
        assertEquals("POST", report.lastRequest?.method)
        assertEquals("/sabr/playback/position", report.lastRequest?.route)
        assertTrue(report.lastSabrSummary?.contains("session=s-0123456789ab") == true)
    }

    @Test
    fun fingerprintIgnoresDynamicMessagesButChangesWithTheCallSite() {
        val first = reportFor(IllegalStateException("token one").withFrames())
        val second = reportFor(IllegalStateException("token two").withFrames())
        val moved = reportFor(
            IllegalStateException("token one").apply {
                stackTrace = arrayOf(StackTraceElement("dev.typetype.android.Player", "enterPip", null, 124))
            },
        )

        assertEquals(first.fingerprint, second.fingerprint)
        assertFalse(first.fingerprint == moved.fingerprint)
        assertTrue(first.fingerprint.matches(Regex("[0-9a-f]{16}")))
    }

    @Test
    fun invalidRequestMetadataIsDropped() {
        val report = CrashReportSanitizer.create(
            throwable = IllegalStateException("private").withFrames(),
            environment = environment(),
            diagnostics = CrashDiagnosticContext(
                lastRequest = CrashRequestMetadata("TRACE", "/watch/private-video-id?token=x", "short"),
                lastSabr = null,
            ),
        )

        assertEquals(null, report.lastRequest)
        assertFalse(report.toString().contains("private-video-id"))
    }

    private fun reportFor(failure: Throwable) = CrashReportSanitizer.create(
        throwable = failure,
        environment = environment(),
        diagnostics = context(),
        timestampEpochMillis = 1_700_000_000_000,
    )

    private fun Throwable.withFrames(): Throwable = apply {
        stackTrace = arrayOf(
            StackTraceElement("dev.typetype.android.Player", "enterPip", "/home/builder/Player.kt", 123),
            StackTraceElement("android.app.Activity", "enterPictureInPictureMode", "Activity.java", 8071),
        )
    }

    private fun environment() = CrashEnvironment(
        appVersion = "1.2.1",
        appVersionCode = 10_201,
        androidVersion = "10",
        apiLevel = 29,
        deviceManufacturer = "samsung",
        deviceModel = "SM-N960F",
    )

    private fun context() = CrashDiagnosticContext(
        lastRequest = CrashRequestMetadata(
            method = "POST",
            route = "/sabr/playback/position",
            requestId = "safe-request-id",
        ),
        lastSabr = SabrDiagnosticDetail(
            sessionFingerprint = "s-0123456789ab",
            generation = 3,
            state = SabrDiagnosticDetail.State.Ready,
            track = SabrDiagnosticDetail.Track.Video,
            blocker = null,
            terminal = null,
            recovery = null,
            bufferProgress = SabrDiagnosticDetail.BufferProgress.Advanced,
        ),
    )
}
