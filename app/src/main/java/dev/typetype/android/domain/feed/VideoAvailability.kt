package dev.typetype.android.domain.feed

enum class VideoAvailability {
    Playable,
    Scheduled,
    MembersOnly,
}

enum class VideoBadgeStatus {
    Live,
    Replay,
    Premiere,
    Upcoming,
}

fun Video.availabilityAt(nowMillis: Long): VideoAvailability = when {
    requiresMembership -> VideoAvailability.MembersOnly
    releaseTimeMillis()?.let { it > nowMillis } == true -> VideoAvailability.Scheduled
    else -> VideoAvailability.Playable
}

fun Video.isPlaybackAvailableAt(nowMillis: Long): Boolean =
    availabilityAt(nowMillis) == VideoAvailability.Playable

fun Video.badgeStatusAt(nowMillis: Long): VideoBadgeStatus? = when {
    isLive -> VideoBadgeStatus.Live
    isPostLive -> VideoBadgeStatus.Replay
    releaseTimeMillis()?.let { it > nowMillis } == true -> VideoBadgeStatus.Premiere
    isLiveContent -> VideoBadgeStatus.Upcoming
    else -> null
}

fun Video.releaseTimeMillis(): Long? =
    publishedAtMillis?.takeIf { it > 0L }
        ?: uploadedAtMillis.takeIf { it > 0L }
