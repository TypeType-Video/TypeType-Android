package dev.typetype.android.domain.preferences

const val DEFAULT_YOUTUBE_BUFFER_SECONDS = 30
val YOUTUBE_BUFFER_SECONDS = listOf(10, 30, 60, 120)

fun normalizeYouTubeBufferSeconds(seconds: Int): Int =
    seconds.takeIf(YOUTUBE_BUFFER_SECONDS::contains) ?: DEFAULT_YOUTUBE_BUFFER_SECONDS

fun youTubeBufferGoalMs(seconds: Int, playbackRate: Float = 1f): Long {
    val rate = playbackRate.takeIf { it.isFinite() && it in 0.25f..4f } ?: 1f
    return (normalizeYouTubeBufferSeconds(seconds) * 1_000L * maxOf(1f, rate))
        .toLong().coerceAtMost(120_000L)
}
