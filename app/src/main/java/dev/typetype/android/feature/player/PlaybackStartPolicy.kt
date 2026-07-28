package dev.typetype.android.feature.player

internal fun replacementSourceStartTimeMs(
    sameMedia: Boolean,
    live: Boolean,
    reusingCurrentSource: Boolean,
    currentMediaTimeMs: Long?,
    requestedStartTimeMs: Long,
): Long = when {
    live && !reusingCurrentSource -> 0L
    sameMedia -> currentMediaTimeMs ?: requestedStartTimeMs.coerceAtLeast(0L)
    else -> requestedStartTimeMs.coerceAtLeast(0L)
}

internal fun replacementPlayerPositionMs(
    sameMedia: Boolean,
    live: Boolean,
    reusingCurrentSource: Boolean,
    currentPositionMs: Long,
    requestedPositionMs: Long,
): Long = when {
    live && !reusingCurrentSource -> 0L
    sameMedia -> currentPositionMs.coerceAtLeast(0L)
    else -> requestedPositionMs.coerceAtLeast(0L)
}
