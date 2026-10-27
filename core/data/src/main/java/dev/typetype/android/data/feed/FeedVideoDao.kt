package dev.typetype.android.data.feed

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FeedVideoDao {
    @Query(
        "SELECT * FROM feed_videos WHERE serverId = :serverId AND accountId = :accountId " +
            "AND feed = :feed ORDER BY position",
    )
    suspend fun get(serverId: String, accountId: String, feed: String): List<FeedVideoEntity>

    @Query(
        "SELECT videoUrl FROM feed_videos WHERE serverId = :serverId AND accountId = :accountId " +
            "AND feed = :feed",
    )
    suspend fun urls(serverId: String, accountId: String, feed: String): List<String>

    @Query(
        "SELECT COALESCE(MAX(position), -1) FROM feed_videos " +
            "WHERE serverId = :serverId AND accountId = :accountId AND feed = :feed",
    )
    suspend fun maxPosition(serverId: String, accountId: String, feed: String): Int

    @Query(
        "DELETE FROM feed_videos WHERE serverId = :serverId AND accountId = :accountId " +
            "AND feed = :feed",
    )
    suspend fun delete(serverId: String, accountId: String, feed: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rows: List<FeedVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(rows: List<FeedVideoEntity>)

    @Query(
        "DELETE FROM feed_videos WHERE serverId = :serverId AND accountId = :accountId " +
            "AND feed = :feed AND position >= :maximumSize",
    )
    suspend fun trim(serverId: String, accountId: String, feed: String, maximumSize: Int)

    @Transaction
    suspend fun replace(
        serverId: String,
        accountId: String,
        feed: String,
        rows: List<FeedVideoEntity>,
    ) {
        delete(serverId, accountId, feed)
        if (rows.isNotEmpty()) insert(rows)
    }

    @Transaction
    suspend fun append(
        serverId: String,
        accountId: String,
        feed: String,
        rows: List<FeedVideoEntity>,
        maximumSize: Int,
    ) {
        val existingUrls = urls(serverId, accountId, feed).toHashSet()
        var position = maxPosition(serverId, accountId, feed) + 1
        val additions = rows
            .filter { existingUrls.add(it.videoUrl) }
            .map { it.copy(position = position++) }
        if (additions.isNotEmpty()) insertNew(additions)
        trim(serverId, accountId, feed, maximumSize)
    }
}
