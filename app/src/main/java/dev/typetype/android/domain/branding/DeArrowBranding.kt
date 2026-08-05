package dev.typetype.android.domain.branding

data class DeArrowTitleCandidate(
    val title: String,
    val original: Boolean,
    val votes: Int,
    val locked: Boolean,
)

data class DeArrowThumbnailCandidate(
    val thumbnailUrl: String?,
    val original: Boolean,
    val votes: Int,
    val locked: Boolean,
)

data class DeArrowItem(
    val videoId: String,
    val legacyTitle: String?,
    val legacyThumbnailUrl: String?,
    val titles: List<DeArrowTitleCandidate>?,
    val thumbnails: List<DeArrowThumbnailCandidate>?,
    val neutralThumbnailUrl: String?,
)

data class VideoBranding(
    val title: String,
    val thumbnailUrl: String,
)

data class DeArrowPreferences(
    val titleMode: String,
    val thumbnailMode: String,
    val trustMode: String,
)

fun resolveDeArrowBranding(
    item: DeArrowItem?,
    fallback: VideoBranding,
    preferences: DeArrowPreferences,
): VideoBranding {
    if (item == null) return fallback
    val titleCandidate = item.titles?.firstOrNull { it.isTrusted(preferences.trustMode) }
    val thumbnailCandidate = item.thumbnails?.firstOrNull { it.isTrusted(preferences.trustMode) }
    val title = when {
        preferences.titleMode == TITLE_MODE_ORIGINAL -> fallback.title
        titleCandidate?.original == true -> fallback.title
        titleCandidate != null -> titleCandidate.title
        item.titles == null && preferences.trustMode == TRUST_MODE_ACCEPTED ->
            item.legacyTitle ?: fallback.title
        else -> fallback.title
    }
    val communityThumbnail = when {
        thumbnailCandidate?.original == true -> fallback.thumbnailUrl
        thumbnailCandidate != null -> thumbnailCandidate.thumbnailUrl ?: fallback.thumbnailUrl
        item.thumbnails == null && preferences.trustMode == TRUST_MODE_ACCEPTED ->
            item.legacyThumbnailUrl
        else -> null
    }
    val thumbnailUrl = when (preferences.thumbnailMode) {
        THUMBNAIL_MODE_DEARROW -> communityThumbnail ?: fallback.thumbnailUrl
        THUMBNAIL_MODE_RANDOM -> item.neutralThumbnailUrl ?: fallback.thumbnailUrl
        THUMBNAIL_MODE_DEARROW_OR_RANDOM ->
            communityThumbnail ?: item.neutralThumbnailUrl ?: fallback.thumbnailUrl
        else -> fallback.thumbnailUrl
    }
    return VideoBranding(title, thumbnailUrl)
}

private fun DeArrowTitleCandidate.isTrusted(mode: String): Boolean =
    if (mode == TRUST_MODE_LOCKED) locked else locked || votes >= 0

private fun DeArrowThumbnailCandidate.isTrusted(mode: String): Boolean =
    if (mode == TRUST_MODE_LOCKED) locked else locked || votes >= 0

private const val TITLE_MODE_ORIGINAL = "original"
private const val THUMBNAIL_MODE_DEARROW = "dearrow"
private const val THUMBNAIL_MODE_RANDOM = "random"
private const val THUMBNAIL_MODE_DEARROW_OR_RANDOM = "dearrow_or_random"
private const val TRUST_MODE_ACCEPTED = "accepted"
private const val TRUST_MODE_LOCKED = "locked"
