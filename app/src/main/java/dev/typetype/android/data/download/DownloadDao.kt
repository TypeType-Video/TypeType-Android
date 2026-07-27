package dev.typetype.android.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query(
        "SELECT * FROM downloads WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY createdAtMillis DESC",
    )
    fun observeAll(serverId: String, accountId: String): Flow<List<DownloadEntity>>

    @Query(
        "SELECT * FROM downloads WHERE serverId = :serverId AND accountId = :accountId " +
            "AND requestId = :requestId LIMIT 1",
    )
    fun observe(serverId: String, accountId: String, requestId: String): Flow<DownloadEntity?>

    @Query(
        "SELECT * FROM downloads WHERE serverId = :serverId AND accountId = :accountId " +
            "AND requestId = :requestId LIMIT 1",
    )
    suspend fun get(serverId: String, accountId: String, requestId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE workId = :workId LIMIT 1")
    suspend fun getByWorkId(workId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query(
        "UPDATE downloads SET serverJobId = :jobId, cached = :cached, status = 'QUEUED', " +
            "updatedAtMillis = :updatedAt WHERE workId = :workId",
    )
    suspend fun attachServerJob(workId: String, jobId: String, cached: Boolean, updatedAt: Long)

    @Query(
        "UPDATE downloads SET status = :status, progressPercent = :progress, stage = :stage, " +
            "errorMessage = :error, updatedAtMillis = :updatedAt WHERE workId = :workId",
    )
    suspend fun updateProgress(
        workId: String,
        status: String,
        progress: Int?,
        stage: String?,
        error: String?,
        updatedAt: Long,
    )

    @Query(
        "UPDATE downloads SET status = 'ENQUEUED', systemDownloadId = :downloadId, " +
            "fileName = :fileName, progressPercent = NULL, stage = NULL, updatedAtMillis = :updatedAt " +
            "WHERE workId = :workId",
    )
    suspend fun markEnqueued(workId: String, downloadId: Long, fileName: String, updatedAt: Long)

    @Query(
        "DELETE FROM downloads WHERE serverId = :serverId AND accountId = :accountId " +
            "AND requestId = :requestId",
    )
    suspend fun delete(serverId: String, accountId: String, requestId: String)
}
