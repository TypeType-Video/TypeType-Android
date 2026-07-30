package dev.typetype.android.domain.playback

data class PlaybackQueueEntry(
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val channelName: String,
)

data class PlaybackQueueState(
    val title: String = "",
    val entries: List<PlaybackQueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
    val isPreparingNext: Boolean = false,
    val failedVideoUrl: String? = null,
    val autoplayCountdown: PlaybackQueueAutoplayCountdown? = null,
) {
    val isActive: Boolean
        get() = currentIndex in entries.indices

    val current: PlaybackQueueEntry?
        get() = entries.getOrNull(currentIndex)

    val next: PlaybackQueueEntry?
        get() = entries.getOrNull(currentIndex + 1) ?: if (
            repeatMode == PlaybackRepeatMode.All && entries.size > 1
        ) {
            entries.firstOrNull()
        } else {
            null
        }
}

data class PlaybackQueueAutoplayCountdown(
    val targetVideoUrl: String,
    val totalMillis: Long,
    val remainingMillis: Long,
    val paused: Boolean,
)
