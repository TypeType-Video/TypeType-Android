package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.Comment
import video.typetype.sdk.core.TypeTypeResult

internal fun TvViewModel.loadInitialComments(videoUrl: String) {
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoadingComments = true)
        when (val result = client.catalog.comments(videoUrl)) {
            is TypeTypeResult.Success -> {
                if (mutableState.value.selectedVideo?.url != videoUrl) return@launch
                mutableState.value = mutableState.value.copy(
                    comments = result.value.comments.usableComments(),
                    commentsNextPage = result.value.nextPage,
                    commentsDisabled = result.value.commentsDisabled,
                    isLoadingComments = false,
                )
            }
            is TypeTypeResult.Failure -> {
                if (mutableState.value.selectedVideo?.url != videoUrl) return@launch
                mutableState.value = mutableState.value.copy(isLoadingComments = false)
            }
        }
    }
}

public fun TvViewModel.loadMoreComments() {
    val videoUrl = mutableState.value.selectedVideo?.url ?: return
    val nextPage = mutableState.value.commentsNextPage ?: return
    if (mutableState.value.isLoadingMoreComments) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoadingMoreComments = true)
        when (val result = client.catalog.comments(videoUrl, nextPage)) {
            is TypeTypeResult.Success -> {
                if (mutableState.value.selectedVideo?.url != videoUrl) return@launch
                mutableState.value = mutableState.value.copy(
                    comments = (mutableState.value.comments + result.value.comments.usableComments())
                        .distinctBy(Comment::id),
                    commentsNextPage = result.value.nextPage,
                    isLoadingMoreComments = false,
                )
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingMoreComments = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.loadCommentReplies(comment: Comment) {
    val videoUrl = mutableState.value.selectedVideo?.url ?: return
    val repliesPage = comment.repliesPage ?: return
    if (comment.id in mutableState.value.loadingCommentReplies) return
    if (mutableState.value.commentReplies[comment.id] != null) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            loadingCommentReplies = mutableState.value.loadingCommentReplies + comment.id,
        )
        when (val result = client.catalog.commentReplies(videoUrl, repliesPage)) {
            is TypeTypeResult.Success -> {
                if (mutableState.value.selectedVideo?.url != videoUrl) return@launch
                mutableState.value = mutableState.value.copy(
                    commentReplies = mutableState.value.commentReplies +
                        (comment.id to result.value.comments.usableComments()),
                    loadingCommentReplies = mutableState.value.loadingCommentReplies - comment.id,
                )
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                loadingCommentReplies = mutableState.value.loadingCommentReplies - comment.id,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

private fun List<Comment>.usableComments(): List<Comment> =
    filter { it.author.isNotBlank() && it.text.isNotBlank() }
