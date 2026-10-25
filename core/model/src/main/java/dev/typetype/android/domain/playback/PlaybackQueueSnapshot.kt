package dev.typetype.android.domain.playback

data class PlaybackQueueSnapshot(
    val serverId: String,
    val accountId: String,
    val title: String,
    val entries: List<PlaybackQueueEntry>,
    val currentIndex: Int,
    val repeatMode: PlaybackRepeatMode,
    val updatedAtMillis: Long,
) {
    val current: PlaybackQueueEntry?
        get() = entries.getOrNull(currentIndex)

    fun toState(): PlaybackQueueState = PlaybackQueueState(
        title = title,
        entries = entries,
        currentIndex = currentIndex,
        repeatMode = repeatMode,
    )
}

interface PlaybackQueueRepository {
    suspend fun get(serverId: String, accountId: String): PlaybackQueueSnapshot?

    suspend fun save(snapshot: PlaybackQueueSnapshot)

    suspend fun clear(serverId: String, accountId: String)
}
