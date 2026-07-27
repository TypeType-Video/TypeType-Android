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
    val youtubeRemoteLoginEnabled: Boolean = false,
    val youtubeRemoteLoginReady: Boolean = false,
    val youtubeRemoteLoginUnavailableReason: String? = null,
)

@Serializable
data class MinClientVersion(
    val android: String? = null,
)
