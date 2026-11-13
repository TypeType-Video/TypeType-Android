package dev.typetype.android.feature.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import dev.typetype.android.R
import dev.typetype.android.domain.comments.Comment

internal val CommentsListPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
internal fun CommentsList(
    items: LazyPagingItems<Comment>,
    replies: CommentRepliesController,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = CommentsListPadding) {
        items(
            count = items.itemCount,
            key = { index -> items.peek(index)?.id ?: "loading-$index" },
        ) { index ->
            val comment = items[index]
            if (comment != null) {
                CommentRow(
                    comment = comment,
                    replies = replies,
                    onUrlClick = onUrlClick,
                    onTimestampClick = onTimestampClick,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            FooterState(items = items)
        }
    }
}

@Composable
internal fun CommentRow(
    comment: Comment,
    replies: CommentRepliesController,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
) {
    val repliesState = replies.stateOf(comment)
    Column(modifier = Modifier.fillMaxWidth()) {
        CommentBody(
            comment = comment,
            avatarSize = 36.dp,
            onUrlClick = onUrlClick,
            onTimestampClick = onTimestampClick,
        )
        if (comment.replyCount > 0 && comment.repliesPage != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clickable { replies.toggle(comment) }
                    .padding(start = 46.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (repliesState is RepliesState.Loaded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.comments_replies_count,
                        comment.replyCount,
                        comment.replyCount,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        CommentReplies(
            state = repliesState,
            onUrlClick = onUrlClick,
            onTimestampClick = onTimestampClick,
            onRetry = { replies.toggle(comment) },
            onLoadMore = { replies.loadMore(comment) },
        )
    }
}
