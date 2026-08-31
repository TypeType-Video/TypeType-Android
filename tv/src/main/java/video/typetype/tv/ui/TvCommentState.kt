package video.typetype.tv.ui

import video.typetype.tv.data.TvAppState

internal fun TvAppState.toCommentsUiState(): CommentsUiState = CommentsUiState(
    comments = comments,
    replies = commentReplies,
    loadingReplies = loadingCommentReplies,
    disabled = commentsDisabled,
    loading = isLoadingComments,
    loadingMore = isLoadingMoreComments,
    canLoadMore = commentsNextPage != null,
)
