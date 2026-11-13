package dev.typetype.android.feature.player.error

import java.io.InterruptedIOException

// Server failure codes first: they name the real cause. The HTTP status is only a fallback, so a YouTube
// block (503) never reads as a missing video and a timeout never reads as a lost connection.
internal fun streamErrorKindFor(code: String?, status: Int?): StreamErrorKind? = when (code) {
    "members_only" -> StreamErrorKind.MemberOnly
    "paid_content" -> StreamErrorKind.PaidContent
    "private_content" -> StreamErrorKind.PrivateContent
    "age_restricted" -> StreamErrorKind.AgeRestricted
    "content_blocked" -> StreamErrorKind.ContentBlocked
    "content_unavailable" -> StreamErrorKind.ContentUnavailable
    "provider_access_blocked", "token_service_unavailable" -> StreamErrorKind.ProviderBlocked
    "upstream_timeout", "sabr_upstream_timeout" -> StreamErrorKind.ServerSlow
    "rate_limited" -> StreamErrorKind.RateLimited
    "scheduled_premiere", "live_not_started" -> StreamErrorKind.ScheduledPremiere
    "youtube_session_needs_reconnect", "youtube_session_unavailable" -> StreamErrorKind.YouTubeSessionRequired
    "android_live_playback_unsupported" -> StreamErrorKind.LiveUnsupported
    "youtube_sabr_preparation_timeout", "android_playback_preparation_timeout" -> StreamErrorKind.SabrPreparationTimedOut
    "android_playback_preparation_failed" -> StreamErrorKind.SabrPreparationFailed
    "android_playback_invalid_index" -> StreamErrorKind.SabrInvalidIndex
    "android_subtitle_inventory_unavailable" -> StreamErrorKind.SubtitleInventoryUnavailable
    "youtube_sabr_contract_mismatch", "youtube_android_playback_incompatible" -> StreamErrorKind.ServerContract
    "no_playable_streams", "youtube_sabr_unavailable", "youtube_sabr_preparation_failed" -> StreamErrorKind.SabrUnavailable
    else -> streamErrorKindForStatus(status)
}

private fun streamErrorKindForStatus(status: Int?): StreamErrorKind? = when (status) {
    401 -> StreamErrorKind.AuthenticationExpired
    403 -> StreamErrorKind.ContentBlocked
    404, 410 -> StreamErrorKind.ContentUnavailable
    429 -> StreamErrorKind.RateLimited
    504 -> StreamErrorKind.ServerSlow
    in 500..599 -> StreamErrorKind.ServerUnavailable
    else -> null
}

// The phone being online is what separates "no connection" from an instance that cannot be reached or is slow.
internal fun transportErrorKind(failures: List<Throwable>, networkAvailable: Boolean): StreamErrorKind = when {
    !networkAvailable -> StreamErrorKind.NetworkUnavailable
    failures.any { it is InterruptedIOException } -> StreamErrorKind.ServerSlow
    else -> StreamErrorKind.InstanceUnreachable
}

internal val StreamErrorKind.isTransient: Boolean
    get() = this == StreamErrorKind.ProviderBlocked ||
        this == StreamErrorKind.ServerSlow ||
        this == StreamErrorKind.ServerUnavailable ||
        this == StreamErrorKind.RateLimited ||
        this == StreamErrorKind.InstanceUnreachable ||
        this == StreamErrorKind.NetworkUnavailable

// Offline failures are retried by the network recovery once the connection is back, not on a timer.
fun isRetryablePlaybackFailure(failure: Throwable, networkAvailable: Boolean): Boolean {
    val kind = classifyStreamError(failure, networkAvailable).kind
    return kind.isTransient && kind != StreamErrorKind.NetworkUnavailable
}
