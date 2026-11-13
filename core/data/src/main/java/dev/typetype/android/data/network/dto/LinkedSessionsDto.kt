package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeDeviceStartResponse(
    val userCode: String,
    val verificationUrl: String,
    val expiresAt: Long = 0,
    val intervalSeconds: Int = 5,
)

@Serializable
data class YoutubeDevicePollResponse(val status: String)

@Serializable
data class BiliBiliSessionStatusResponse(val status: String = "disconnected")

@Serializable
data class BiliBiliQrLoginResponse(
    val qrUrl: String,
    val qrcodeKey: String,
    val expiresAt: Long = 0,
)

@Serializable
data class BiliBiliQrPollRequest(val qrcodeKey: String)

@Serializable
data class BiliBiliQrPollResponse(
    val status: String,
    val message: String = "",
)
