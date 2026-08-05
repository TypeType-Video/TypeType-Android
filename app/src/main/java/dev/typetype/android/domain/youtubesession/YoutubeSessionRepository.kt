package dev.typetype.android.domain.youtubesession

interface YoutubeSessionRepository {
    suspend fun getStatus(): Result<YoutubeSession>
    suspend fun startRemoteBrowser(returnTo: String? = null): Result<YoutubeRemoteBrowserSession>
    suspend fun cancelRemoteBrowser(sessionId: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
}
