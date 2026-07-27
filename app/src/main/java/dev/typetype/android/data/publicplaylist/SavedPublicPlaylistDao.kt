package dev.typetype.android.data.publicplaylist

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPublicPlaylistDao {
    @Query(
        "SELECT * FROM saved_public_playlists " +
            "WHERE serverId = :serverId AND accountId = :accountId ORDER BY savedAtMillis DESC",
    )
    fun observe(serverId: String, accountId: String): Flow<List<SavedPublicPlaylistEntity>>

    @Upsert
    suspend fun upsert(entity: SavedPublicPlaylistEntity)

    @Query(
        "DELETE FROM saved_public_playlists WHERE serverId = :serverId " +
            "AND accountId = :accountId AND id = :id",
    )
    suspend fun delete(serverId: String, accountId: String, id: String)

    @Query(
        "DELETE FROM saved_public_playlists WHERE serverId = :serverId AND accountId = :accountId",
    )
    suspend fun deleteAll(serverId: String, accountId: String)

    @Transaction
    suspend fun replaceAll(
        serverId: String,
        accountId: String,
        rows: List<SavedPublicPlaylistEntity>,
    ) {
        deleteAll(serverId, accountId)
        rows.forEach { upsert(it) }
    }
}
