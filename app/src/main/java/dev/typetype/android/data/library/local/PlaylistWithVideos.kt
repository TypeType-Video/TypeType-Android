package dev.typetype.android.data.library.local

import androidx.room.Embedded
import androidx.room.Relation
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo

data class PlaylistWithVideos(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "cacheKey",
        entityColumn = "playlistCacheKey",
    )
    val videos: List<PlaylistVideoEntity>,
) {
    fun toDomain(): Playlist = Playlist(
        id = playlist.id,
        name = playlist.name,
        description = playlist.description,
        createdAtMillis = playlist.createdAtMillis,
        videoCount = maxOf(playlist.videoCount, videos.size),
        videos = videos
            .sortedWith(compareBy<PlaylistVideoEntity> { it.position }.thenBy { it.id })
            .distinctBy { it.url }
            .map { v ->
                PlaylistVideo(
                    id = v.id,
                    url = v.url,
                    title = v.title,
                    thumbnailUrl = v.thumbnailUrl,
                    durationSeconds = v.durationSeconds,
                    position = v.position,
                    channelName = v.channelName,
                    channelUrl = v.channelUrl,
                    channelAvatarUrl = v.channelAvatarUrl,
                    viewCount = v.viewCount,
                )
            },
    )
}
