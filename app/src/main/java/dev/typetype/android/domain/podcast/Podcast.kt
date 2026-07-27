package dev.typetype.android.domain.podcast

import dev.typetype.android.domain.feed.Video

data class Podcast(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val episodeCount: Long,
    val type: String,
)

data class ChannelPodcastPage(
    val channelName: String,
    val channelUrl: String,
    val podcasts: List<Podcast>,
    val episodes: List<Video>,
    val nextPage: String?,
)

data class PodcastEpisodesPage(
    val podcast: Podcast,
    val episodes: List<Video>,
    val nextPage: String?,
)
