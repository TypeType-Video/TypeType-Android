package dev.typetype.android.domain.stream

interface StreamRepository {
    suspend fun loadStream(videoUrl: String): Result<Stream>

    suspend fun loadPlaybackStream(videoUrl: String): Result<Stream> = loadStream(videoUrl)

    suspend fun loadPlaybackMetadata(videoUrl: String): Result<Stream>? = null
}
