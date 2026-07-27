package dev.typetype.android.data.publicplaylist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist

@Entity(
    tableName = "saved_public_playlists",
    primaryKeys = ["serverId", "accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["serverId", "accountId"]),
        Index(value = ["serverId", "accountId", "url"], unique = true),
    ],
)
data class SavedPublicPlaylistEntity(
    val serverId: String,
    val accountId: String,
    val id: String,
    val publicPlaylistId: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val streamCount: Long,
    val playlistType: String,
    val savedAtMillis: Long,
) {
    fun toDomain() = SavedPublicPlaylist(
        id = id,
        publicPlaylistId = publicPlaylistId,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        streamCount = streamCount,
        playlistType = playlistType,
        savedAtMillis = savedAtMillis,
    )
}
