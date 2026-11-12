package dev.typetype.android.domain.linkedsessions

data class YoutubeDeviceLogin(
    val userCode: String,
    val verificationUrl: String,
    val expiresAtMillis: Long,
    val intervalSeconds: Int,
)

enum class YoutubeDevicePoll { Pending, SlowDown, Connected }

interface YoutubeDeviceLoginRepository {
    suspend fun start(): Result<YoutubeDeviceLogin>
    suspend fun poll(): Result<YoutubeDevicePoll>
    suspend fun cancel(): Result<Unit>
}

enum class BiliBiliSessionState { Connected, NeedsReconnect, Disconnected }

data class BiliBiliQrLogin(
    val qrUrl: String,
    val qrcodeKey: String,
    val expiresAtMillis: Long,
)

enum class BiliBiliQrPoll { Waiting, Scanned, Confirmed, Expired, Failed }

interface BiliBiliSessionRepository {
    suspend fun state(): Result<BiliBiliSessionState>
    suspend fun startQrLogin(): Result<BiliBiliQrLogin>
    suspend fun pollQrLogin(qrcodeKey: String): Result<BiliBiliQrPoll>
    suspend fun disconnect(): Result<Unit>
}
