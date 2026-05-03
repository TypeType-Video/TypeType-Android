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
    val apiVersion: Int,
    val registrationAllowed: Boolean,
    val guestAllowed: Boolean,
    val supportedServices: List<Int> = emptyList(),
    val minClientVersion: MinClientVersion? = null,
)

@Serializable
data class MinClientVersion(
    val android: Int? = null,
)
