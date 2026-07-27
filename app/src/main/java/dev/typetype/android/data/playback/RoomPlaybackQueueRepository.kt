package dev.typetype.android.data.playback

import dev.typetype.android.domain.playback.PlaybackQueueRepository
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackRepeatMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPlaybackQueueRepository @Inject constructor(
    private val dao: PlaybackQueueDao,
) : PlaybackQueueRepository {
    override suspend fun get(serverId: String, accountId: String): PlaybackQueueSnapshot? {
        val rows = dao.get(serverId, accountId)
        val first = rows.firstOrNull() ?: return null
        if (first.currentIndex !in rows.indices) {
            dao.delete(serverId, accountId)
            return null
        }
        return PlaybackQueueSnapshot(
            serverId = serverId,
            accountId = accountId,
            title = first.queueTitle,
            entries = rows.map(PlaybackQueueEntity::toEntry),
            currentIndex = first.currentIndex,
            repeatMode = PlaybackRepeatMode.fromStorage(first.repeatMode),
            updatedAtMillis = first.updatedAtMillis,
        )
    }

    override suspend fun save(snapshot: PlaybackQueueSnapshot) {
        require(snapshot.serverId.isNotBlank())
        require(snapshot.accountId.isNotBlank())
        require(snapshot.currentIndex in snapshot.entries.indices)
        val rows = snapshot.entries.mapIndexed { position, entry ->
            require(entry.videoUrl.isNotBlank())
            PlaybackQueueEntity(
                serverId = snapshot.serverId,
                accountId = snapshot.accountId,
                position = position,
                currentIndex = snapshot.currentIndex,
                queueTitle = snapshot.title,
                repeatMode = snapshot.repeatMode.name,
                videoUrl = entry.videoUrl,
                title = entry.title,
                thumbnailUrl = entry.thumbnailUrl,
                durationSeconds = entry.durationSeconds.coerceAtLeast(0L),
                channelName = entry.channelName,
                updatedAtMillis = snapshot.updatedAtMillis,
            )
        }
        dao.replace(snapshot.serverId, snapshot.accountId, rows)
    }

    override suspend fun clear(serverId: String, accountId: String) {
        dao.delete(serverId, accountId)
    }
}
