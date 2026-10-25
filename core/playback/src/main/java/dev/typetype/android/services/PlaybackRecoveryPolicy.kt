package dev.typetype.android.services

internal class PlaybackRecoveryPolicy(
    val windowMs: Long = 120_000L,
    private val baseDelayMs: Long = 1_000L,
    private val maxDelayMs: Long = 8_000L,
) {
    fun delayMs(attempt: Int): Long =
        (baseDelayMs shl (attempt - 1).coerceIn(0, MAX_SHIFT)).coerceAtMost(maxDelayMs)

    fun isExhausted(elapsedMs: Long): Boolean = elapsedMs >= windowMs

    private companion object {
        const val MAX_SHIFT = 6
    }
}
