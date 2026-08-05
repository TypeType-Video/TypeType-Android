package dev.typetype.android.domain.branding

interface DeArrowRepository {
    suspend fun load(sourceUrl: String, durationSeconds: Long): Result<DeArrowItem?>
}
