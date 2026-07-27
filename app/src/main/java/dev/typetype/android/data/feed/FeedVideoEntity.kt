package dev.typetype.android.data.feed

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.feed.Video

@Entity(
    tableName = "feed_videos",
    primaryKeys = ["serverId", "accountId", "feed", "videoUrl"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["serverId", "accountId", "feed"]),
        Index(value = ["serverId", "accountId", "feed", "position"], unique = true),
    ],
)
data class FeedVideoEntity(
    val serverId: String,
    val accountId: String,
    val feed: String,
    val position: Int,
    val videoUrl: String,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String,
    val uploaderAvatarUrl: String,
    val uploaderVerified: Boolean,
    val durationSeconds: Long,
    val isLive: Boolean,
    val viewCount: Long,
    val uploadedAtMillis: Long,
    val isShortFormContent: Boolean,
    val shortDescription: String?,
    val publishedAtMillis: Long?,
    val isPostLive: Boolean,
    val isLiveContent: Boolean,
    val requiresMembership: Boolean,
    val savedAtMillis: Long,
)

fun Video.toFeedEntity(
    scope: AccountScope,
    feed: String,
    position: Int,
    savedAtMillis: Long,
): FeedVideoEntity = FeedVideoEntity(
    serverId = scope.serverId,
    accountId = scope.accountId,
    feed = feed,
    position = position,
    videoUrl = url,
    videoId = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl,
    uploaderAvatarUrl = uploaderAvatarUrl,
    uploaderVerified = uploaderVerified,
    durationSeconds = durationSeconds,
    isLive = isLive,
    viewCount = viewCount,
    uploadedAtMillis = uploadedAtMillis,
    isShortFormContent = isShortFormContent,
    shortDescription = shortDescription,
    publishedAtMillis = publishedAtMillis,
    isPostLive = isPostLive,
    isLiveContent = isLiveContent,
    requiresMembership = requiresMembership,
    savedAtMillis = savedAtMillis,
)

fun FeedVideoEntity.toDomainVideo(): Video = Video(
    id = videoId,
    url = videoUrl,
    title = title,
    thumbnailUrl = thumbnailUrl,
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl,
    uploaderAvatarUrl = uploaderAvatarUrl,
    uploaderVerified = uploaderVerified,
    durationSeconds = durationSeconds,
    isLive = isLive,
    viewCount = viewCount,
    uploadedAtMillis = uploadedAtMillis,
    isShortFormContent = isShortFormContent,
    shortDescription = shortDescription,
    publishedAtMillis = publishedAtMillis,
    isPostLive = isPostLive,
    isLiveContent = isLiveContent,
    requiresMembership = requiresMembership,
)
