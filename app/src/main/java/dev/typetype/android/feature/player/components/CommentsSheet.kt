package dev.typetype.android.feature.player.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private sealed interface RepliesState {
    data object Loading : RepliesState
    data class Loaded(val replies: List<Comment>) : RepliesState
    data class Failed(val message: String) : RepliesState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    pagingFlow: Flow<PagingData<Comment>>,
    videoUrl: String,
    commentsRepository: CommentsRepository,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items: LazyPagingItems<Comment> = pagingFlow.collectAsLazyPagingItems()
    val context = LocalContext.current
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val repliesByCommentId = remember { mutableStateMapOf<String, RepliesState>() }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            CommentsList(
                items = items,
                repliesByCommentId = repliesByCommentId,
                onUrlClick = { pendingUrl = it },
                onToggleReplies = { comment ->
                val current = repliesByCommentId[comment.id]
                when (current) {
                    is RepliesState.Loaded -> repliesByCommentId.remove(comment.id)
                    RepliesState.Loading -> Unit
                    else -> {
                        val replyCursor = comment.repliesPage ?: return@CommentsList
                        repliesByCommentId[comment.id] = RepliesState.Loading
                        coroutineScope.launch {
                            commentsRepository.loadReplies(videoUrl, replyCursor)
                                .fold(
                                    onSuccess = {
                                        repliesByCommentId[comment.id] =
                                            RepliesState.Loaded(it.comments)
                                    },
                                    onFailure = {
                                        repliesByCommentId[comment.id] = RepliesState.Failed(
                                            it.message ?: "Could not load replies",
                                        )
                                    },
                                )
                        }
                    }
                }
            },
        )
        }
    }

    if (pendingUrl != null) {
        ExternalLinkDialog(
            url = pendingUrl!!,
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pendingUrl))
                context.startActivity(intent)
                pendingUrl = null
            },
            onDismiss = { pendingUrl = null },
        )
    }
}

@Composable
private fun CommentsList(
    items: LazyPagingItems<Comment>,
    repliesByCommentId: Map<String, RepliesState>,
    onUrlClick: (String) -> Unit,
    onToggleReplies: (Comment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        items(
            count = items.itemCount,
            key = { index -> items.peek(index)?.id ?: "loading-$index" },
        ) { index ->
            val comment = items[index]
            if (comment != null) {
                val repliesState = repliesByCommentId[comment.id]
                CommentRow(
                    comment = comment,
                    repliesState = repliesState,
                    onUrlClick = onUrlClick,
                    onToggleReplies = { onToggleReplies(comment) },
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
private fun CommentRow(
    comment: Comment,
    repliesState: RepliesState?,
    onUrlClick: (String) -> Unit,
    onToggleReplies: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CommentBody(
            comment = comment,
            avatarSize = 36.dp,
            onUrlClick = onUrlClick,
        )
        if (comment.replyCount > 0 && comment.repliesPage != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleReplies)
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
                    text = "${comment.replyCount} replies",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        when (repliesState) {
            RepliesState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            is RepliesState.Loaded -> {
                Column(modifier = Modifier.padding(start = 46.dp, top = 4.dp)) {
                    repliesState.replies.forEach { reply ->
                        CommentBody(
                            comment = reply,
                            avatarSize = 28.dp,
                            onUrlClick = onUrlClick,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            is RepliesState.Failed -> {
                Text(
                    text = repliesState.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 46.dp, top = 4.dp),
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun CommentBody(
    comment: Comment,
    avatarSize: androidx.compose.ui.unit.Dp,
    onUrlClick: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comment.uploaderVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = comment.publishedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinkedText(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                linkColor = MaterialTheme.colorScheme.primary,
                onUrlClick = onUrlClick,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (comment.likeCount > 0) {
                    Text(
                        text = comment.textualLikeCount.ifBlank { comment.likeCount.toString() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterState(items: LazyPagingItems<Comment>) {
    val state = items.loadState
    when {
        state.append.endOfPaginationReached && items.itemCount == 0 -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.append is androidx.paging.LoadState.Loading ||
            state.refresh is androidx.paging.LoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
