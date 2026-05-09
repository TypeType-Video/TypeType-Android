package dev.typetype.android.data.library.local

import androidx.room.Embedded
import androidx.room.Relation
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo

data class PlaylistWithVideos(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
    )
    val videos: List<PlaylistVideoEntity>,
) {
    fun toDomain(): Playlist = Playlist(
        id = playlist.id,
        name = playlist.name,
        description = playlist.description,
        createdAtMillis = playlist.createdAtMillis,
        videos = videos
            .sortedBy { it.position }
            .map { v ->
                PlaylistVideo(
                    id = v.id,
                    url = v.url,
                    title = v.title,
                    thumbnailUrl = v.thumbnailUrl,
                    durationSeconds = v.durationSeconds,
                    position = v.position,
                )
            },
    )
}
