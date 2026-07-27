package dev.typetype.android.domain.publicplaylist

data class SavedPublicPlaylist(
    val id: String,
    val publicPlaylistId: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val streamCount: Long,
    val playlistType: String,
    val savedAtMillis: Long,
)
