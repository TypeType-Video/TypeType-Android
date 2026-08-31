package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Comment

internal data class CommentsUiState(
    val comments: List<Comment>,
    val replies: Map<String, List<Comment>>,
    val loadingReplies: Set<String>,
    val disabled: Boolean,
    val loading: Boolean,
    val loadingMore: Boolean,
    val canLoadMore: Boolean,
)

@Composable
internal fun CommentsPanel(
    state: CommentsUiState,
    onLoadReplies: (Comment) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val closeFocus = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(Unit) { closeFocus.requestFocus() }
    Surface(
        modifier = Modifier.fillMaxWidth(.42f).fillMaxHeight(),
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xFF111214)),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Comments",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (state.comments.isNotEmpty()) {
                    Text(
                        state.comments.size.toString() + if (state.canLoadMore) "+" else "",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = .5f),
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(56.dp).focusRequester(closeFocus),
                    onClick = onDismiss,
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = .1f),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close comments")
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.disabled -> PanelMessage("Comments are disabled for this video.")
                    state.loading && state.comments.isEmpty() -> PanelMessage("Loading comments")
                    state.comments.isEmpty() -> PanelMessage("No comments are available.")
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        items(state.comments, key = Comment::id) { comment ->
                            TvComment(
                                comment = comment,
                                replies = state.replies[comment.id].orEmpty(),
                                loadingReplies = comment.id in state.loadingReplies,
                                onLoadReplies = { onLoadReplies(comment) },
                            )
                        }
                        if (state.canLoadMore || state.loadingMore) {
                            item {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onLoadMore,
                                    enabled = !state.loadingMore,
                                ) {
                                    Text(if (state.loadingMore) "Loading" else "Load more comments")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvComment(
    comment: Comment,
    replies: List<Comment>,
    loadingReplies: Boolean,
    onLoadReplies: () -> Unit,
) {
    var expanded by remember(comment.id) { mutableStateOf(false) }
    var showReplies by remember(comment.id) { mutableStateOf(false) }
    val canExpand = comment.text.length > 240 || comment.text.count { it == '\n' } >= 4
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (canExpand) expanded = !expanded },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = .045f),
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = .13f),
            focusedContentColor = Color.White,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                CommentAvatar(comment)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.author,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                        )
                        if (comment.uploaderVerified) CommentTag("Verified")
                        if (comment.isPinned) CommentTag("Pinned")
                        if (comment.isHeartedByUploader) CommentTag("Creator heart")
                    }
                    comment.relativePublishedTime().takeIf(String::isNotBlank)?.let { publishedTime ->
                        Text(publishedTime, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .55f))
                    }
                    Text(
                        comment.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = .9f),
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (canExpand) {
                        Text(
                            if (expanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val likes = comment.textualLikeCount.ifBlank {
                        comment.likeCount.takeIf { it >= 0 }?.toString().orEmpty()
                    }
                    if (likes.isNotBlank()) {
                        Text("$likes likes", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .55f))
                    }
                    if (comment.repliesPage != null || replies.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (replies.isEmpty()) onLoadReplies()
                                showReplies = !showReplies
                            },
                            enabled = !loadingReplies,
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when {
                                    loadingReplies -> "Loading"
                                    showReplies -> "Hide replies"
                                    else -> repliesLabel(comment.replyCount)
                                },
                            )
                        }
                    }
                }
            }
            if (showReplies) replies.forEach { reply ->
                Row(modifier = Modifier.padding(start = 46.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CommentAvatar(reply, 30)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            reply.author,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(reply.text, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentAvatar(comment: Comment, size: Int = 38) {
    if (comment.authorAvatarUrl.isBlank()) {
        Box(Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)))
    } else {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = comment.author,
            modifier = Modifier.size(size.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun CommentTag(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 8.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = .72f),
    )
}

@Composable
private fun PanelMessage(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun repliesLabel(count: Int): String = if (count > 0) "$count replies" else "Show replies"
