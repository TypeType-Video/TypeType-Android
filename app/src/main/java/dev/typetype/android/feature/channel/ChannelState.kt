package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.podcast.Podcast

data class ChannelState(
    val isLoading: Boolean = true,
    val channel: Channel? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val isSubscribed: Boolean = false,
    val subscribeInFlight: Boolean = false,
    val podcasts: List<Podcast> = emptyList(),
    val podcastsLoading: Boolean = false,
)
