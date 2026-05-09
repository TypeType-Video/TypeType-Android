package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistsDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAtMillis DESC")
    fun observeAllWithVideos(): Flow<List<PlaylistWithVideos>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideos(videos: List<PlaylistVideoEntity>)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    @Query("DELETE FROM playlist_videos")
    suspend fun deleteAllVideos()

    @Transaction
    suspend fun replaceAll(playlists: List<PlaylistEntity>, videos: List<PlaylistVideoEntity>) {
        deleteAllVideos()
        deleteAllPlaylists()
        playlists.forEach { upsertPlaylist(it) }
        if (videos.isNotEmpty()) upsertVideos(videos)
    }
}
