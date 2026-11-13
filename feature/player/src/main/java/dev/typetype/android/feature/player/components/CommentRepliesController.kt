package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.rememberUserErrorMapper
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class CommentRepliesController(
    private val scope: CoroutineScope,
    private val failureMessage: (Throwable) -> String,
    private val loadReplies: suspend (String) -> Result<CommentsPage>,
) {
    private val states = mutableStateMapOf<String, RepliesState>()

    fun stateOf(comment: Comment): RepliesState? = states[comment.id]

    fun toggle(comment: Comment) {
        when (states[comment.id]) {
            is RepliesState.Loaded -> states.remove(comment.id)
            RepliesState.Loading -> Unit
            is RepliesState.Failed, null -> load(comment)
        }
    }

    fun loadMore(comment: Comment) {
        val current = states[comment.id] as? RepliesState.Loaded ?: return
        val nextPage = current.nextPage ?: return
        if (current.isLoadingMore) return
        states[comment.id] = current.copy(isLoadingMore = true, loadMoreFailed = false)
        scope.launch {
            val result = loadReplies(nextPage)
            val latest = states[comment.id] as? RepliesState.Loaded ?: return@launch
            states[comment.id] = result.fold(
                onSuccess = { page ->
                    latest.copy(
                        replies = mergeCommentsById(latest.replies, page.comments),
                        nextPage = page.nextpage,
                        isLoadingMore = false,
                    )
                },
                onFailure = { latest.copy(isLoadingMore = false, loadMoreFailed = true) },
            )
        }
    }

    private fun load(comment: Comment) {
        val cursor = comment.repliesPage ?: return
        states[comment.id] = RepliesState.Loading
        scope.launch {
            states[comment.id] = loadReplies(cursor).fold(
                onSuccess = { RepliesState.Loaded(replies = it.comments, nextPage = it.nextpage) },
                onFailure = { RepliesState.Failed(failureMessage(it)) },
            )
        }
    }
}

@Composable
internal fun rememberCommentRepliesController(
    videoUrl: String,
    loadReplies: suspend (videoUrl: String, cursor: String) -> Result<CommentsPage>,
): CommentRepliesController {
    val scope = rememberCoroutineScope()
    val errorMapper = rememberUserErrorMapper()
    return remember(videoUrl, scope, errorMapper) {
        CommentRepliesController(scope, { errorMapper.message(it, R.string.comments_replies_failed) }) { cursor ->
            loadReplies(videoUrl, cursor)
        }
    }
}
