package dev.typetype.android.domain.playback

data class PlaybackResume(
    val serverId: String,
    val accountId: String,
    val videoUrl: String,
    val positionMillis: Long,
    val updatedAtMillis: Long,
)

interface PlaybackResumeRepository {
    suspend fun get(serverId: String, accountId: String): PlaybackResume?

    suspend fun save(resume: PlaybackResume)

    suspend fun clear(serverId: String, accountId: String)
}
