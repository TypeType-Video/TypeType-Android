package dev.typetype.android.domain.server

data class RssCapability(
    val enabled: Boolean = false,
    val maxFeedsPerUser: Int = 0,
    val maxItems: Int = 0,
    val minimumPollMinutes: Int = 0,
    val rateLimitPerMinute: Int = 0,
)
