package dev.typetype.android.data.playback

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlaybackResumeDao {
    @Query(
        "SELECT * FROM playback_resume WHERE serverId = :serverId AND accountId = :accountId",
    )
    suspend fun get(serverId: String, accountId: String): PlaybackResumeEntity?

    @Upsert
    suspend fun upsert(entry: PlaybackResumeEntity)

    @Query(
        "DELETE FROM playback_resume WHERE serverId = :serverId AND accountId = :accountId",
    )
    suspend fun delete(serverId: String, accountId: String)
}
