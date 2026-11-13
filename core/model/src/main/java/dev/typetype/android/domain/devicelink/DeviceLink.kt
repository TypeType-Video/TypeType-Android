package dev.typetype.android.domain.devicelink

data class DeviceLinkRequest(
    val userCode: String,
    val deviceName: String,
    val expiresAtMillis: Long,
)

interface DeviceLinkRepository {
    suspend fun lookup(userCode: String): Result<DeviceLinkRequest>
    suspend fun approve(userCode: String): Result<Unit>
    suspend fun deny(userCode: String): Result<Unit>
}
