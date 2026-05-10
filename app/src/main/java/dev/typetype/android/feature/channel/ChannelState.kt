package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.Channel

data class ChannelState(
    val isLoading: Boolean = true,
    val channel: Channel? = null,
    val errorMessage: String? = null,
    val isSubscribed: Boolean = false,
    val subscribeInFlight: Boolean = false,
)
