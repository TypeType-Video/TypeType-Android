package dev.typetype.android.domain.comments

interface BulletCommentsRepository {
    suspend fun load(videoUrl: String): Result<List<BulletComment>>
}
