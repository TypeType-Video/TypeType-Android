package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceLookupResponse(
    val userCode: String,
    val deviceName: String = "",
    val expiresAt: Long = 0,
)

@Serializable
data class DeviceUserCodeRequest(val userCode: String)
