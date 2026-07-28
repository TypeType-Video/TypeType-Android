package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline

internal fun Player.sabrMediaTimeMs(windowPositionMs: Long, expectedLive: Boolean): Long? {
    val positionMs = windowPositionMs.coerceAtLeast(0L)
    if (!expectedLive) return positionMs
    val timeline = currentTimeline
    val mediaItemIndex = currentMediaItemIndex
    if (timeline.isEmpty || mediaItemIndex !in 0 until timeline.windowCount) return null
    val window = timeline.getWindow(mediaItemIndex, Timeline.Window())
    return sabrMediaTimeMs(
        windowPositionMs = positionMs,
        expectedLive = true,
        placeholder = window.isPlaceholder,
        timelineLive = window.isLive,
        positionInFirstPeriodMs = window.positionInFirstPeriodMs,
    )
}

internal fun Player.currentLiveTargetPositionMs(): Long? {
    val timeline = currentTimeline
    val mediaItemIndex = currentMediaItemIndex
    if (timeline.isEmpty || mediaItemIndex !in 0 until timeline.windowCount) return null
    val window = timeline.getWindow(mediaItemIndex, Timeline.Window())
    return window.defaultPositionMs.takeIf {
        window.isLive && !window.isPlaceholder && it != C.TIME_UNSET && it >= 0L
    }
}

internal fun sabrMediaTimeMs(
    windowPositionMs: Long,
    expectedLive: Boolean,
    placeholder: Boolean,
    timelineLive: Boolean,
    positionInFirstPeriodMs: Long,
): Long? {
    val positionMs = windowPositionMs.coerceAtLeast(0L)
    if (!expectedLive) return positionMs
    if (placeholder || !timelineLive) return null
    return saturatedAdd(positionMs, positionInFirstPeriodMs.coerceAtLeast(0L))
}

internal fun sabrWindowPositionMs(
    mediaTimeMs: Long,
    liveActive: Boolean,
    seekableStartMs: Long,
    seekableEndMs: Long,
): Long {
    val positionMs = mediaTimeMs.coerceAtLeast(0L)
    if (!liveActive) return positionMs
    val startMs = seekableStartMs.coerceAtLeast(0L)
    val endMs = seekableEndMs.coerceAtLeast(startMs)
    return (positionMs.coerceIn(startMs, endMs) - startMs).coerceAtLeast(0L)
}

private fun saturatedAdd(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
