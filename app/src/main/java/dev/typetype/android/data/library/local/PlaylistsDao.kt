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

    @Query("SELECT id FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findIdByName(name: String): String?

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM playlist_videos pv " +
            "INNER JOIN playlists p ON pv.playlistId = p.id " +
            "WHERE p.name = :playlistName COLLATE NOCASE AND pv.url = :videoUrl" +
            ")",
    )
    fun observeIsVideoInPlaylistNamed(playlistName: String, videoUrl: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideos(videos: List<PlaylistVideoEntity>)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    @Query("DELETE FROM playlist_videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND url = :videoUrl")
    suspend fun deleteVideoFromPlaylist(playlistId: String, videoUrl: String)

    @Transaction
    suspend fun replaceAll(playlists: List<PlaylistEntity>, videos: List<PlaylistVideoEntity>) {
        deleteAllVideos()
        deleteAllPlaylists()
        playlists.forEach { upsertPlaylist(it) }
        if (videos.isNotEmpty()) upsertVideos(videos)
    }
}
