package dev.typetype.android.domain.comments

interface CommentsRepository {
    suspend fun loadPage(videoUrl: String, nextpage: String?): Result<CommentsPage>
    suspend fun loadReplies(videoUrl: String, repliesPage: String): Result<CommentsPage>
}
