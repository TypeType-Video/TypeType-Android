package dev.typetype.android.data.playback

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PlaybackQueueDao {
    @Query(
        "SELECT * FROM playback_queue WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY position ASC",
    )
    suspend fun get(serverId: String, accountId: String): List<PlaybackQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entries: List<PlaybackQueueEntity>)

    @Query(
        "DELETE FROM playback_queue WHERE serverId = :serverId AND accountId = :accountId",
    )
    suspend fun delete(serverId: String, accountId: String)

    @Transaction
    suspend fun replace(
        serverId: String,
        accountId: String,
        entries: List<PlaybackQueueEntity>,
    ) {
        delete(serverId, accountId)
        if (entries.isNotEmpty()) insert(entries)
    }
}
