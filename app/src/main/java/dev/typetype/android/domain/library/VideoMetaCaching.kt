package dev.typetype.android.domain.library

import dev.typetype.android.domain.feed.Video

suspend fun VideoMetaRepository.cacheVideos(videos: List<Video>) {
    val metas = videos
        .filter { it.uploaderUrl.isNotBlank() || it.uploaderAvatarUrl.isNotBlank() }
        .map {
            VideoMeta(
                videoUrl = it.url,
                channelName = it.uploaderName,
                channelUrl = it.uploaderUrl,
                channelAvatarUrl = it.uploaderAvatarUrl,
                viewCount = it.viewCount,
            )
        }
    if (metas.isNotEmpty()) putAll(metas)
}
