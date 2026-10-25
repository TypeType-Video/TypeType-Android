package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.comments.Comment

internal sealed interface RepliesState {
    data object Loading : RepliesState

    data class Loaded(
        val replies: List<Comment>,
        val nextPage: String?,
        val isLoadingMore: Boolean = false,
        val loadMoreFailed: Boolean = false,
    ) : RepliesState

    data class Failed(val message: String) : RepliesState
}

internal fun mergeCommentReplies(
    current: List<Comment>,
    additions: List<Comment>,
): List<Comment> = buildList {
    val ids = mutableSetOf<String>()
    (current + additions).forEach { comment ->
        if (ids.add(comment.id)) add(comment)
    }
}

@Composable
internal fun CommentReplies(
    state: RepliesState?,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Crossfade(targetState = state, label = "comment replies") { visibleState ->
            Column(modifier = Modifier.padding(start = 46.dp, top = 6.dp)) {
                when (visibleState) {
                    RepliesState.Loading -> repeat(2) {
                        CommentSkeleton(avatarSize = 28.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                    is RepliesState.Loaded -> LoadedReplies(
                        state = visibleState,
                        onUrlClick = onUrlClick,
                        onTimestampClick = onTimestampClick,
                        onLoadMore = onLoadMore,
                    )
                    is RepliesState.Failed -> {
                        Text(
                            text = visibleState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.state_retry))
                        }
                    }
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun LoadedReplies(
    state: RepliesState.Loaded,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    state.replies.forEach { reply ->
        CommentBody(
            comment = reply,
            avatarSize = 28.dp,
            onUrlClick = onUrlClick,
            onTimestampClick = onTimestampClick,
        )
        Spacer(Modifier.height(10.dp))
    }
    if (state.isLoadingMore) {
        CommentSkeleton(avatarSize = 28.dp)
    } else if (state.nextPage != null || state.loadMoreFailed) {
        TextButton(onClick = onLoadMore) {
            Text(
                stringResource(
                    if (state.loadMoreFailed) R.string.state_retry
                    else R.string.comments_load_more_replies,
                ),
            )
        }
    }
}
