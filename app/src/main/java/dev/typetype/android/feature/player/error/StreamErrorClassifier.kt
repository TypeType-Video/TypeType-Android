package dev.typetype.android.feature.player.error

import dev.typetype.android.core.error.CodedFailure
import java.io.IOException

private val MEMBER_ONLY_NEEDLES = listOf(
    "only available for members",
    "members-only",
    "member only",
    "requires membership",
    "premium content",
)

private val YOUTUBE_SESSION_NEEDLES = listOf(
    "sign in is required to verify access",
    "login is required to verify access",
)

private val SCHEDULED_PREMIERE_NEEDLES = listOf(
    "premieres in",
    "premiere has not started",
    "premiere scheduled",
)

private val PAID_CONTENT_NEEDLES = listOf(
    "paid video",
    "payment required",
    "youtube music premium",
)

private val SABR_UNAVAILABLE_NEEDLES = listOf(
    "sabr bootstrap metadata unavailable",
    "sabr playback formats unavailable",
)

private const val GENERIC_EXTRACTOR_ERROR =
    "Error occurs when fetching the page. Try increase the loading timeout in Settings."

fun isMemberOnlyMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val value = message.lowercase()
    if (MEMBER_ONLY_NEEDLES.any { it in value }) return true
    if (message == GENERIC_EXTRACTOR_ERROR) return true
    return false
}

fun isYouTubeSessionRequiredMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val value = message.lowercase()
    return YOUTUBE_SESSION_NEEDLES.any { it in value }
}

private fun isScheduledPremiereMessage(message: String?): Boolean =
    message?.lowercase()?.let { value -> SCHEDULED_PREMIERE_NEEDLES.any { it in value } } == true

private fun isPaidContentMessage(message: String?): Boolean =
    message?.lowercase()?.let { value -> PAID_CONTENT_NEEDLES.any { it in value } } == true

private fun isSabrUnavailableMessage(message: String?): Boolean =
    message?.lowercase()?.let { value -> SABR_UNAVAILABLE_NEEDLES.any { it in value } } == true

private val GEO_PATTERN = Regex("only available in (.+)$", RegexOption.IGNORE_CASE)

private val COUNTRY_NAME_TO_ISO = mapOf(
    "Afghanistan" to "AF", "Albania" to "AL", "Algeria" to "DZ", "Andorra" to "AD",
    "Angola" to "AO", "Argentina" to "AR", "Armenia" to "AM", "Australia" to "AU",
    "Austria" to "AT", "Azerbaijan" to "AZ", "Bahrain" to "BH", "Bangladesh" to "BD",
    "Belarus" to "BY", "Belgium" to "BE", "Bolivia" to "BO", "Bosnia and Herzegovina" to "BA",
    "Brazil" to "BR", "Bulgaria" to "BG", "Cambodia" to "KH", "Cameroon" to "CM",
    "Canada" to "CA", "Chile" to "CL", "China" to "CN", "Colombia" to "CO",
    "Croatia" to "HR", "Cuba" to "CU", "Cyprus" to "CY", "Czech Republic" to "CZ",
    "Czechia" to "CZ", "Denmark" to "DK", "Ecuador" to "EC", "Egypt" to "EG",
    "Estonia" to "EE", "Ethiopia" to "ET", "Finland" to "FI", "France" to "FR",
    "Georgia" to "GE", "Germany" to "DE", "Ghana" to "GH", "Greece" to "GR",
    "Guatemala" to "GT", "Honduras" to "HN", "Hong Kong" to "HK", "Hungary" to "HU",
    "Iceland" to "IS", "India" to "IN", "Indonesia" to "ID", "Iran" to "IR",
    "Iraq" to "IQ", "Ireland" to "IE", "Israel" to "IL", "Italy" to "IT",
    "Jamaica" to "JM", "Japan" to "JP", "Jordan" to "JO", "Kazakhstan" to "KZ",
    "Kenya" to "KE", "Kosovo" to "XK", "Kuwait" to "KW", "Kyrgyzstan" to "KG",
    "Laos" to "LA", "Latvia" to "LV", "Lebanon" to "LB", "Libya" to "LY",
    "Lithuania" to "LT", "Luxembourg" to "LU", "Malaysia" to "MY", "Malta" to "MT",
    "Mexico" to "MX", "Moldova" to "MD", "Mongolia" to "MN", "Montenegro" to "ME",
    "Morocco" to "MA", "Myanmar" to "MM", "Nepal" to "NP", "Netherlands" to "NL",
    "New Zealand" to "NZ", "Nicaragua" to "NI", "Nigeria" to "NG", "North Korea" to "KP",
    "North Macedonia" to "MK", "Norway" to "NO", "Oman" to "OM", "Pakistan" to "PK",
    "Palestine" to "PS", "Panama" to "PA", "Paraguay" to "PY", "Peru" to "PE",
    "Philippines" to "PH", "Poland" to "PL", "Portugal" to "PT", "Qatar" to "QA",
    "Romania" to "RO", "Russia" to "RU", "Saudi Arabia" to "SA", "Senegal" to "SN",
    "Serbia" to "RS", "Singapore" to "SG", "Slovakia" to "SK", "Slovenia" to "SI",
    "Somalia" to "SO", "South Africa" to "ZA", "South Korea" to "KR", "Spain" to "ES",
    "Sri Lanka" to "LK", "Sudan" to "SD", "Sweden" to "SE", "Switzerland" to "CH",
    "Syria" to "SY", "Taiwan" to "TW", "Tajikistan" to "TJ", "Tanzania" to "TZ",
    "Thailand" to "TH", "Tunisia" to "TN", "Turkey" to "TR", "Turkmenistan" to "TM",
    "Uganda" to "UG", "Ukraine" to "UA", "United Arab Emirates" to "AE",
    "United Kingdom" to "GB", "United States" to "US", "Uruguay" to "UY",
    "Uzbekistan" to "UZ", "Venezuela" to "VE", "Vietnam" to "VN", "Yemen" to "YE",
    "Zimbabwe" to "ZW",
)

fun parseGeoRestriction(message: String?): String? {
    if (message.isNullOrBlank()) return null
    val match = GEO_PATTERN.find(message) ?: return null
    val countryName = match.groupValues[1].trim()
    return COUNTRY_NAME_TO_ISO[countryName]
}

enum class StreamErrorKind {
    Generic,
    AuthenticationExpired,
    MemberOnly,
    PaidContent,
    ScheduledPremiere,
    GeoRestricted,
    YouTubeSessionRequired,
    SabrUnavailable,
    SabrInvalidIndex,
    SabrPreparationFailed,
    SabrPreparationTimedOut,
    SubtitleInventoryUnavailable,
    ServerContract,
    NetworkUnavailable,
    LiveUnsupported,
}

data class StreamErrorClass(
    val kind: StreamErrorKind,
    val countryCode: String? = null,
    val rawMessage: String?,
    val requestId: String? = null,
)

fun classifyStreamError(message: String?): StreamErrorClass {
    val safe = message?.takeIf { it.isNotBlank() }
    val geo = parseGeoRestriction(safe)
    if (geo != null) {
        return StreamErrorClass(StreamErrorKind.GeoRestricted, geo, rawMessage = safe)
    }
    if (isYouTubeSessionRequiredMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.YouTubeSessionRequired, countryCode = null, rawMessage = safe)
    }
    if (isScheduledPremiereMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.ScheduledPremiere, rawMessage = safe)
    }
    if (isPaidContentMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.PaidContent, rawMessage = safe)
    }
    if (isMemberOnlyMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.MemberOnly, countryCode = null, rawMessage = safe)
    }
    if (isSabrUnavailableMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.SabrUnavailable, rawMessage = safe)
    }
    return StreamErrorClass(StreamErrorKind.Generic, countryCode = null, rawMessage = safe)
}

fun classifyStreamError(failure: Throwable): StreamErrorClass {
    val failures = failure.causeChain()
    val coded = failures.filterIsInstance<CodedFailure>().firstOrNull()
    val code = coded?.failureCode
    val requestId = coded?.requestId
    if (coded?.statusCode == 401) {
        return StreamErrorClass(
            kind = StreamErrorKind.AuthenticationExpired,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code == "members_only") {
        return StreamErrorClass(StreamErrorKind.MemberOnly, rawMessage = null, requestId = requestId)
    }
    if (code == "paid_content") {
        return StreamErrorClass(StreamErrorKind.PaidContent, rawMessage = null, requestId = requestId)
    }
    if (code == "scheduled_premiere") {
        return StreamErrorClass(StreamErrorKind.ScheduledPremiere, rawMessage = null, requestId = requestId)
    }
    if (code == "youtube_session_needs_reconnect" || code == "youtube_session_unavailable") {
        return StreamErrorClass(
            kind = StreamErrorKind.YouTubeSessionRequired,
            countryCode = null,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code == "android_live_playback_unsupported") {
        return StreamErrorClass(
            kind = StreamErrorKind.LiveUnsupported,
            countryCode = null,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (
        code == "youtube_sabr_preparation_timeout" ||
        code == "android_playback_preparation_timeout"
    ) {
        return StreamErrorClass(
            kind = StreamErrorKind.SabrPreparationTimedOut,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code == "android_playback_preparation_failed") {
        return StreamErrorClass(
            kind = StreamErrorKind.SabrPreparationFailed,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code == "android_playback_invalid_index") {
        return StreamErrorClass(
            kind = StreamErrorKind.SabrInvalidIndex,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code == "android_subtitle_inventory_unavailable") {
        return StreamErrorClass(
            kind = StreamErrorKind.SubtitleInventoryUnavailable,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code in SABR_CONTRACT_CODES) {
        return StreamErrorClass(
            kind = StreamErrorKind.ServerContract,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (code in SABR_UNAVAILABLE_CODES) {
        return StreamErrorClass(
            kind = StreamErrorKind.SabrUnavailable,
            countryCode = null,
            rawMessage = null,
            requestId = requestId,
        )
    }
    if (failures.any { it is IOException }) {
        return StreamErrorClass(
            kind = StreamErrorKind.NetworkUnavailable,
            rawMessage = null,
            requestId = requestId,
        )
    }
    failures.forEach { candidate ->
        val classified = classifyStreamError(candidate.message)
        if (classified.kind != StreamErrorKind.Generic) {
            return classified.copy(requestId = requestId)
        }
    }
    return classifyStreamError(failure.message).copy(requestId = requestId)
}

private fun Throwable.causeChain(): List<Throwable> {
    val result = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && current !in result && result.size < MAX_CAUSE_DEPTH) {
        result += current
        current = current.cause
    }
    return result
}

private val SABR_UNAVAILABLE_CODES = setOf(
    "no_playable_streams",
    "youtube_sabr_unavailable",
    "youtube_sabr_preparation_failed",
)

private val SABR_CONTRACT_CODES = setOf(
    "youtube_sabr_contract_mismatch",
    "youtube_android_playback_incompatible",
)

private const val MAX_CAUSE_DEPTH = 12
