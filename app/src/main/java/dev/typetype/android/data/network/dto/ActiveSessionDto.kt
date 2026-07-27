package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionDeviceRequest(
    val clientName: String,
    val clientVersion: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
)

@Serializable
data class SessionPlaybackRequest(
    val clientName: String,
    val clientVersion: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val videoUrl: String,
    val title: String,
    val thumbnail: String? = null,
    val channelName: String? = null,
    val positionMs: Long,
    val durationMs: Long? = null,
    val paused: Boolean,
)
