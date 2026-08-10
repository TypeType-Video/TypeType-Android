package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class InstanceResponse(
    val name: String,
    val tagline: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val version: String,
    val revision: String = "",
    val shortRevision: String = "",
    val buildTime: String = "",
    val apiVersion: Int,
    val registrationAllowed: Boolean,
    val guestAllowed: Boolean,
    val supportedServices: List<Int> = emptyList(),
    val minClientVersion: MinClientVersion? = null,
    val localLoginEnabled: Boolean = true,
    val oidcEnabled: Boolean = false,
    val oidcProviderName: String? = null,
    val oidcAutoRedirect: Boolean = false,
    val youtubeRemoteLoginEnabled: Boolean? = null,
    val youtubeRemoteLoginReady: Boolean? = null,
    val youtubeRemoteLoginUnavailableReason: String? = null,
    val rss: RssInstanceCapabilityDto? = null,
)

@Serializable
data class RssInstanceCapabilityDto(
    val enabled: Boolean = false,
    val maxFeedsPerUser: Int = 0,
    val maxItems: Int = 0,
    val minimumPollMinutes: Int = 0,
    val rateLimitPerMinute: Int = 0,
)

@Serializable
data class MinClientVersion(
    val android: String? = null,
)
