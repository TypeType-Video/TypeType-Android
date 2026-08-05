package dev.typetype.android.domain.stream

interface AudioOnlyStreamRepository {
    suspend fun resolve(
        requestScope: StreamRequestScope,
        videoUrl: String,
        preferOriginal: Boolean,
        preferredLocale: String,
    ): Result<AudioOnlyStream>
}
