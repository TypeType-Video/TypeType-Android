package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import okhttp3.Response

@Singleton
class SabrDiagnosticSanitizer @Inject constructor(
    private val json: Json,
) {
    private val bufferedEdges = object : LinkedHashMap<String, Long>(MAX_SESSIONS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > MAX_SESSIONS
    }

    fun sanitize(route: String, request: Request, response: Response): SabrDiagnosticDetail? {
        if (route !in SABR_ROUTES) return null
        val payload = responsePayload(route, response)
        val sessionId = payload?.text("sessionId") ?: request.sessionId(route) ?: return null
        val fingerprint = fingerprint(sessionId)
        val edge = payload?.number("bufferedEdgeMs")
        val ready = payload?.flag("ready") == true
        val status = payload?.text("status")
        val blockerText = payload?.text("blockedBy")
        val terminalText = payload?.text("terminalError")
        return SabrDiagnosticDetail(
            sessionFingerprint = fingerprint,
            generation = payload?.number("generation")?.takeIf { it >= 0L },
            state = state(status, ready),
            track = track(blockerText),
            blocker = blocker(blockerText),
            terminal = terminal(response.code, terminalText),
            recovery = recovery(payload?.text("recoveryAction")),
            bufferProgress = bufferProgress(fingerprint, edge),
        )
    }

    fun sanitizeFailure(route: String, request: Request): SabrDiagnosticDetail? {
        if (route !in SABR_ROUTES) return null
        val sessionId = request.sessionId(route) ?: return null
        return SabrDiagnosticDetail(
            sessionFingerprint = fingerprint(sessionId),
            generation = null,
            state = null,
            track = null,
            blocker = null,
            terminal = null,
            recovery = null,
            bufferProgress = null,
        )
    }

    private fun responsePayload(route: String, response: Response): JsonObject? {
        if (route !in CONTROL_ROUTES) return null
        val contentType = response.body.contentType()
        if (contentType != null && contentType.subtype.lowercase(Locale.ROOT) !in JSON_SUBTYPES) {
            return null
        }
        return try {
            json.parseToJsonElement(response.peekBody(MAX_BODY_BYTES).string()).jsonObject
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun state(value: String?, ready: Boolean): SabrDiagnosticDetail.State? {
        if (ready) return SabrDiagnosticDetail.State.Ready
        return when (value?.lowercase(Locale.ROOT)?.replace('_', '-')) {
            "idle" -> SabrDiagnosticDetail.State.Idle
            "preparing" -> SabrDiagnosticDetail.State.Preparing
            "requesting" -> SabrDiagnosticDetail.State.Requesting
            "repositioning" -> SabrDiagnosticDetail.State.Repositioning
            "waiting-for-live", "waiting-live" -> SabrDiagnosticDetail.State.WaitingForLive
            "throttled" -> SabrDiagnosticDetail.State.Throttled
            "network-failed" -> SabrDiagnosticDetail.State.NetworkFailed
            "terminal", "failed" -> SabrDiagnosticDetail.State.Terminal
            "stopped" -> SabrDiagnosticDetail.State.Stopped
            "ready" -> SabrDiagnosticDetail.State.Ready
            else -> null
        }
    }

    private fun track(value: String?): SabrDiagnosticDetail.Track? {
        val normalized = value?.lowercase(Locale.ROOT) ?: return null
        return when {
            normalized.startsWith("audio:") -> SabrDiagnosticDetail.Track.Audio
            normalized.startsWith("video:") -> SabrDiagnosticDetail.Track.Video
            else -> null
        }
    }

    private fun blocker(value: String?): SabrDiagnosticDetail.Blocker? {
        val normalized = value?.lowercase(Locale.ROOT) ?: return null
        return when {
            "no-media" in normalized || "no media" in normalized ->
                SabrDiagnosticDetail.Blocker.ProtectedNoMedia
            "token" in normalized || "forbidden" in normalized || "unauthorized" in normalized ->
                SabrDiagnosticDetail.Blocker.Token
            "pending" in normalized -> SabrDiagnosticDetail.Blocker.SegmentPending
            "discontinuity" in normalized -> SabrDiagnosticDetail.Blocker.Discontinuity
            "window capped" in normalized -> SabrDiagnosticDetail.Blocker.WindowCapped
            "reload" in normalized -> SabrDiagnosticDetail.Blocker.Reload
            "policy" in normalized -> SabrDiagnosticDetail.Blocker.Policy
            "upstream" in normalized || "request failed" in normalized ->
                SabrDiagnosticDetail.Blocker.Upstream
            else -> SabrDiagnosticDetail.Blocker.Other
        }
    }

    private fun terminal(code: Int, value: String?): SabrDiagnosticDetail.Terminal? {
        when (code) {
            404 -> return SabrDiagnosticDetail.Terminal.MissingSession
            409 -> return SabrDiagnosticDetail.Terminal.StaleGeneration
            410 -> return SabrDiagnosticDetail.Terminal.ExpiredSession
        }
        val normalized = value?.lowercase(Locale.ROOT) ?: return null
        return when {
            "no-media" in normalized || "no media" in normalized ->
                SabrDiagnosticDetail.Terminal.ProtectedNoMedia
            "token" in normalized || "forbidden" in normalized || "unauthorized" in normalized ||
                "403" in normalized -> SabrDiagnosticDetail.Terminal.Token
            "demand stalled" in normalized || "segment" in normalized && "stalled" in normalized ->
                SabrDiagnosticDetail.Terminal.SegmentStalled
            "ump" in normalized -> SabrDiagnosticDetail.Terminal.UmpResponse
            "network" in normalized || "timeout" in normalized || "upstream" in normalized ->
                SabrDiagnosticDetail.Terminal.Upstream
            else -> SabrDiagnosticDetail.Terminal.Other
        }
    }

    private fun recovery(value: String?): SabrDiagnosticDetail.Recovery? = when (value) {
        "retry_fresh_session" -> SabrDiagnosticDetail.Recovery.FreshSession
        "retry_fresh_session_lower_video_itag" -> SabrDiagnosticDetail.Recovery.LowerVideoFormat
        null, "" -> null
        else -> SabrDiagnosticDetail.Recovery.Other
    }

    private fun bufferProgress(
        fingerprint: String,
        currentEdge: Long?,
    ): SabrDiagnosticDetail.BufferProgress? {
        currentEdge ?: return null
        if (currentEdge < 0L) return null
        return synchronized(bufferedEdges) {
            val previous = bufferedEdges.put(fingerprint, currentEdge)
            when {
                currentEdge == 0L && previous == null -> SabrDiagnosticDetail.BufferProgress.Empty
                previous == null -> SabrDiagnosticDetail.BufferProgress.Initial
                currentEdge > previous -> SabrDiagnosticDetail.BufferProgress.Advanced
                currentEdge < previous -> SabrDiagnosticDetail.BufferProgress.Regressed
                else -> SabrDiagnosticDetail.BufferProgress.Stalled
            }
        }
    }

    private fun Request.sessionId(route: String): String? {
        if (route == CREATE_ROUTE) return null
        val segments = url.pathSegments
        val playbackIndex = segments.indexOfLast { it == "playback" }
        return segments.getOrNull(playbackIndex + 1)?.takeIf {
            playbackIndex >= 0 && it.isNotBlank()
        }
    }

    private fun fingerprint(sessionId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sessionId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "s-${digest.take(FINGERPRINT_LENGTH)}"
    }

    private fun JsonObject.text(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private fun JsonObject.number(name: String): Long? = (get(name) as? JsonPrimitive)?.longOrNull

    private fun JsonObject.flag(name: String): Boolean? = (get(name) as? JsonPrimitive)?.booleanOrNull

    private companion object {
        const val MAX_BODY_BYTES = 16L * 1024L
        const val MAX_SESSIONS = 64
        const val FINGERPRINT_LENGTH = 12
        const val CREATE_ROUTE = "/sabr/playback/create"
        val CONTROL_ROUTES = setOf(
            CREATE_ROUTE,
            "/sabr/playback/seek",
            "/sabr/playback/position",
            "/sabr/playback/prefetch",
            "/sabr/playback/segments",
            "/sabr/playback/window",
            "/sabr/playback/state",
        )
        val SABR_ROUTES = CONTROL_ROUTES + setOf(
            "/sabr/playback/init",
            "/sabr/playback/segment",
        )
        val JSON_SUBTYPES = setOf("json", "problem+json")
    }
}
