package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoMetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<VideoMetaEntity>)

    @Query(
        "SELECT * FROM video_meta WHERE serverId = :serverId AND accountId = :accountId " +
            "AND videoUrl IN (:urls)",
    )
    fun observeForUrls(serverId: String, accountId: String, urls: List<String>): Flow<List<VideoMetaEntity>>
}
