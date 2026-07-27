package dev.typetype.android.data.library.sync

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.library.LibraryCollection

@Entity(
    tableName = "library_mutation_outbox",
    primaryKeys = ["serverId", "accountId", "mutationKey"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("serverId", "accountId", "sessionGeneration", "state"),
        Index("serverId", "accountId", "collection"),
    ],
)
data class LibraryMutationEntity(
    val serverId: String,
    val accountId: String,
    val mutationKey: String,
    val collection: String,
    val kind: String,
    val targetId: String,
    val parentId: String?,
    val desiredPresent: Boolean,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val channelName: String,
    val channelUrl: String,
    val channelAvatarUrl: String,
    val viewCount: Long,
    val sessionGeneration: Long,
    val mutationVersion: Long,
    val state: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastAttemptAtMillis: Long?,
    val attemptCount: Int,
    val failureCode: String?,
    val failureStatusCode: Int?,
    val requestId: String?,
)

enum class LibraryMutationKind(
    val storageKey: String,
    val collection: LibraryCollection,
) {
    Favorite("favorite", LibraryCollection.Favorites),
    WatchLater("watch_later", LibraryCollection.WatchLater),
    Subscription("subscription", LibraryCollection.Subscriptions),
    PlaylistVideo("playlist_video", LibraryCollection.Playlists),
}

internal fun libraryMutationKey(kind: LibraryMutationKind, parentId: String?, targetId: String): String {
    val parent = parentId.orEmpty()
    return "${kind.storageKey}:${parent.length}:$parent$targetId"
}

internal fun LibraryMutationEntity.scope(): AccountScope = AccountScope(serverId, accountId)

internal const val MUTATION_PENDING = "pending"
internal const val MUTATION_FAILED = "failed"
