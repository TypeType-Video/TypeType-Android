package dev.typetype.android.domain.stream

interface StreamRepository {
    suspend fun loadStream(videoUrl: String): Result<Stream>
}
