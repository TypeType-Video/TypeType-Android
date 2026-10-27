package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistsDao {
    @Transaction
    @Query(
        "SELECT * FROM playlists WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY createdAtMillis DESC",
    )
    fun observeAllWithVideos(serverId: String, accountId: String): Flow<List<PlaylistWithVideos>>

    @Query(
        "SELECT id FROM playlists WHERE serverId = :serverId AND accountId = :accountId " +
            "AND name = :name COLLATE NOCASE LIMIT 1",
    )
    suspend fun findIdByName(serverId: String, accountId: String, name: String): String?

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE cacheKey = :playlistCacheKey)")
    suspend fun containsPlaylist(playlistCacheKey: String): Boolean

    @Query("SELECT * FROM playlists WHERE cacheKey = :playlistCacheKey LIMIT 1")
    suspend fun getPlaylist(playlistCacheKey: String): PlaylistEntity?

    @Query(
        "SELECT id FROM playlist_videos WHERE playlistCacheKey = :playlistCacheKey " +
            "AND url = :videoUrl LIMIT 1",
    )
    suspend fun findVideoId(playlistCacheKey: String, videoUrl: String): String?

    @Query(
        "SELECT url FROM playlist_videos WHERE playlistCacheKey = :playlistCacheKey " +
            "ORDER BY position ASC, id ASC",
    )
    suspend fun getVideoUrls(playlistCacheKey: String): List<String>

    @Query(
        "SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_videos " +
            "WHERE playlistCacheKey = :playlistCacheKey",
    )
    suspend fun nextVideoPosition(playlistCacheKey: String): Int

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM playlist_videos pv " +
            "INNER JOIN playlists p ON pv.playlistCacheKey = p.cacheKey " +
            "WHERE p.serverId = :serverId AND p.accountId = :accountId " +
            "AND p.name = :playlistName COLLATE NOCASE AND pv.url = :videoUrl" +
            ")",
    )
    fun observeIsVideoInPlaylistNamed(
        serverId: String,
        accountId: String,
        playlistName: String,
        videoUrl: String,
    ): Flow<Boolean>

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideos(videos: List<PlaylistVideoEntity>)

    @Query("DELETE FROM playlists WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAllPlaylists(serverId: String, accountId: String)

    @Query("DELETE FROM playlists WHERE cacheKey = :playlistCacheKey")
    suspend fun deletePlaylist(playlistCacheKey: String)

    @Query("UPDATE playlists SET name = :name WHERE cacheKey = :playlistCacheKey")
    suspend fun renamePlaylist(playlistCacheKey: String, name: String)

    @Query(
        "DELETE FROM playlists WHERE serverId = :serverId AND accountId = :accountId " +
            "AND id NOT IN (:playlistIds)",
    )
    suspend fun deleteMissingPlaylists(serverId: String, accountId: String, playlistIds: List<String>)

    @Query(
        "DELETE FROM playlist_videos WHERE playlistCacheKey IN " +
            "(SELECT cacheKey FROM playlists WHERE serverId = :serverId AND accountId = :accountId)",
    )
    suspend fun deleteAllVideos(serverId: String, accountId: String)

    @Query("DELETE FROM playlist_videos WHERE playlistCacheKey = :playlistCacheKey")
    suspend fun deleteVideos(playlistCacheKey: String)

    @Query("DELETE FROM playlist_videos WHERE playlistCacheKey = :playlistCacheKey AND url = :videoUrl")
    suspend fun deleteVideoFromPlaylist(playlistCacheKey: String, videoUrl: String)

    @Query(
        "UPDATE playlist_videos SET position = :position " +
            "WHERE playlistCacheKey = :playlistCacheKey AND url = :videoUrl",
    )
    suspend fun updateVideoPosition(playlistCacheKey: String, videoUrl: String, position: Int)

    @Query(
        "UPDATE playlists SET videoCount = max(0, videoCount + :delta) " +
            "WHERE cacheKey = :playlistCacheKey",
    )
    suspend fun adjustVideoCount(playlistCacheKey: String, delta: Int)

    @Transaction
    suspend fun replaceAll(
        serverId: String,
        accountId: String,
        playlists: List<PlaylistEntity>,
        videos: List<PlaylistVideoEntity>,
    ) {
        deleteAllVideos(serverId, accountId)
        deleteAllPlaylists(serverId, accountId)
        playlists.forEach { upsertPlaylist(it) }
        if (videos.isNotEmpty()) upsertVideos(videos)
    }

    @Transaction
    suspend fun replaceSummaries(
        serverId: String,
        accountId: String,
        playlists: List<PlaylistEntity>,
    ) {
        val ids = playlists.map { it.id }
        if (ids.isEmpty()) deleteAllPlaylists(serverId, accountId)
        else deleteMissingPlaylists(serverId, accountId, ids)
        playlists.forEach { upsertPlaylist(it) }
    }

    @Transaction
    suspend fun replaceDetail(
        playlist: PlaylistEntity,
        videos: List<PlaylistVideoEntity>,
    ) {
        upsertPlaylist(playlist)
        deleteVideos(playlist.cacheKey)
        if (videos.isNotEmpty()) upsertVideos(videos)
    }

    @Transaction
    suspend fun reorderVideos(playlistCacheKey: String, orderedVideoUrls: List<String>) {
        orderedVideoUrls.forEachIndexed { position, videoUrl ->
            updateVideoPosition(playlistCacheKey, videoUrl, position)
        }
    }
}
