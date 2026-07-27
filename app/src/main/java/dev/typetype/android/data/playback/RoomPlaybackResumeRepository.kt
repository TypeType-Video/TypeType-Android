package dev.typetype.android.data.playback

import dev.typetype.android.domain.playback.PlaybackResume
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPlaybackResumeRepository @Inject constructor(
    private val dao: PlaybackResumeDao,
) : PlaybackResumeRepository {
    override suspend fun get(serverId: String, accountId: String): PlaybackResume? =
        dao.get(serverId, accountId)?.toDomain()

    override suspend fun save(resume: PlaybackResume) {
        require(resume.serverId.isNotBlank())
        require(resume.accountId.isNotBlank())
        require(resume.videoUrl.isNotBlank())
        require(resume.positionMillis >= 0L)
        dao.upsert(PlaybackResumeEntity.fromDomain(resume))
    }

    override suspend fun clear(serverId: String, accountId: String) {
        dao.delete(serverId, accountId)
    }
}
