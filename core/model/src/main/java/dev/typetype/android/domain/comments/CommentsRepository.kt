package dev.typetype.android.domain.comments

interface CommentsRepository {
    suspend fun loadPage(videoUrl: String, nextpage: String?): Result<CommentsPage>
    suspend fun loadReplies(videoUrl: String, repliesPage: String): Result<CommentsPage>
    suspend fun searchComments(videoUrl: String, query: String, cursor: String?): Result<CommentSearchPage>
    suspend fun loadVoiceReplyAudio(voiceReplyUrl: String): Result<ByteArray>
}
