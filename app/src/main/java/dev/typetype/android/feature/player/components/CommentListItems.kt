package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.comments.Comment

@Composable
internal fun CommentBody(
    comment: Comment,
    avatarSize: Dp,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
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
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
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
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                linkColor = MaterialTheme.colorScheme.primary,
                onUrlClick = onUrlClick,
                onTimestampClick = onTimestampClick,
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
internal fun FooterState(items: LazyPagingItems<Comment>) {
    val state = items.loadState
    when {
        state.append.endOfPaginationReached && items.itemCount == 0 -> Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.comments_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.append is androidx.paging.LoadState.Loading ||
            state.refresh is androidx.paging.LoadState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
