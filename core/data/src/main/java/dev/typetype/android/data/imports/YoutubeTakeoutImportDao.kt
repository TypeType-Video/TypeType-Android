package dev.typetype.android.data.imports

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface YoutubeTakeoutImportDao {
    @Query(
        "SELECT * FROM youtube_takeout_imports " +
            "WHERE serverId = :serverId AND accountId = :accountId ORDER BY createdAtMillis DESC",
    )
    fun observeAll(serverId: String, accountId: String): Flow<List<YoutubeTakeoutImportEntity>>

    @Query("SELECT * FROM youtube_takeout_imports WHERE workId = :workId LIMIT 1")
    suspend fun getByWorkId(workId: String): YoutubeTakeoutImportEntity?

    @Query(
        "SELECT * FROM youtube_takeout_imports WHERE serverId = :serverId " +
            "AND accountId = :accountId AND requestId = :requestId LIMIT 1",
    )
    suspend fun get(serverId: String, accountId: String, requestId: String): YoutubeTakeoutImportEntity?

    @Upsert
    suspend fun upsert(entry: YoutubeTakeoutImportEntity)

    @Query(
        "UPDATE youtube_takeout_imports SET serverJobId = :jobId, status = 'PARSING', " +
            "phase = :phase, progressPercent = :progress, updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun attachServerJob(
        workId: String,
        jobId: String,
        phase: String,
        progress: Int,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE youtube_takeout_imports SET status = :status, phase = :phase, " +
            "progressPercent = :progress, updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun updateProgress(
        workId: String,
        status: String,
        phase: String?,
        progress: Int?,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE youtube_takeout_imports SET previewSubscriptions = :subscriptions, " +
            "previewPlaylists = :playlists, previewPlaylistItems = :playlistItems, " +
            "previewFavorites = :favorites, previewWatchLater = :watchLater, " +
            "previewHistory = :history, warningCount = :warnings, errorCount = :errors, " +
            "updatedAtMillis = :updatedAt WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun savePreview(
        workId: String,
        subscriptions: Int,
        playlists: Int,
        playlistItems: Int,
        favorites: Int,
        watchLater: Int,
        history: Int,
        warnings: Int,
        errors: Int,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE youtube_takeout_imports SET status = 'COMPLETED', phase = 'completed', " +
            "progressPercent = 100, importedCount = :imported, skippedCount = :skipped, " +
            "failedCount = :failed, warningCount = :warnings, errorCount = :errors, " +
            "failureCode = NULL, failureRequestId = NULL, collectionsRefreshed = 0, " +
            "updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun complete(
        workId: String,
        imported: Int,
        skipped: Int,
        failed: Int,
        warnings: Int,
        errors: Int,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE youtube_takeout_imports SET status = 'FAILED', phase = NULL, " +
            "progressPercent = NULL, failureCode = :code, failureRequestId = :requestId, " +
            "updatedAtMillis = :updatedAt WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun fail(workId: String, code: String, requestId: String?, updatedAt: Long): Int

    @Query(
        "UPDATE youtube_takeout_imports SET status = 'QUEUED', failureCode = :code, " +
            "failureRequestId = :requestId, updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId AND status != 'CANCELLED'",
    )
    suspend fun retrying(workId: String, code: String, requestId: String?, updatedAt: Long): Int

    @Query(
        "UPDATE youtube_takeout_imports SET status = 'CANCELLED', phase = NULL, " +
            "progressPercent = NULL, failureCode = :code, updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId AND status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')",
    )
    suspend fun cancel(workId: String, code: String, updatedAt: Long): Int

    @Query(
        "UPDATE youtube_takeout_imports SET collectionsRefreshed = 1, updatedAtMillis = :updatedAt " +
            "WHERE serverId = :serverId AND accountId = :accountId AND requestId = :requestId " +
            "AND status = 'COMPLETED'",
    )
    suspend fun markCollectionsRefreshed(
        serverId: String,
        accountId: String,
        requestId: String,
        updatedAt: Long,
    )

    @Query(
        "DELETE FROM youtube_takeout_imports WHERE serverId = :serverId " +
            "AND accountId = :accountId AND requestId = :requestId",
    )
    suspend fun delete(serverId: String, accountId: String, requestId: String)
}
