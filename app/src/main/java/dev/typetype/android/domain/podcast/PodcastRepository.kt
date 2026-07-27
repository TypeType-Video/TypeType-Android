package dev.typetype.android.domain.podcast

interface PodcastRepository {
    suspend fun channelPodcasts(
        channelUrl: String,
        nextPage: String? = null,
    ): Result<ChannelPodcastPage>

    suspend fun episodes(
        podcastUrl: String,
        nextPage: String? = null,
    ): Result<PodcastEpisodesPage>
}
