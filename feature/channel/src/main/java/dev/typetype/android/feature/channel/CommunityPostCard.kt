package dev.typetype.android.feature.channel

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.richtext.RichMarkupText
import dev.typetype.android.core.ui.richtext.richMarkupPlainText
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.url.buildImageUrl
import dev.typetype.android.domain.channel.CommunityPost

@Composable
internal fun CommunityPostCard(
    post: CommunityPost,
    onPlayVideo: (String) -> Unit,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nested: Boolean = false,
) {
    val shape = if (nested) MaterialTheme.shapes.medium else MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (nested) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            )
            .padding(if (nested) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PostHeader(post = post, avatarSize = if (nested) 28.dp else 40.dp)
        if (post.text.isNotBlank()) PostText(text = post.text, onUrlClick = onUrlClick)
        PostImages(images = post.images)
        post.poll?.let { PostPoll(it) }
        post.video?.let { PostVideo(video = it, onPlay = { onPlayVideo(it.url) }) }
        post.sharedPost?.let {
            CommunityPostCard(post = it, onPlayVideo = onPlayVideo, onUrlClick = onUrlClick, nested = true)
        }
        if (!nested) PostFooter(post)
    }
}

@Composable
private fun PostHeader(post: CommunityPost, avatarSize: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = buildImageUrl(LocalServerBaseUrl.current, post.authorAvatarUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = post.authorName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            postPublishedLabel(post)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PostText(text: String, onUrlClick: (String) -> Unit) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    val foldable = richMarkupPlainText(text).length > POST_FOLD_CHARACTERS
    Column {
        RichMarkupText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            linkColor = MaterialTheme.colorScheme.primary,
            onUrlClick = onUrlClick,
            maxLines = if (foldable && !expanded) POST_FOLD_LINES else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        if (foldable) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.comments_show_less else R.string.comments_read_more))
            }
        }
    }
}

@Composable
private fun PostFooter(post: CommunityPost) {
    if (post.likeLabel.isEmpty() && post.commentLabel.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (post.likeLabel.isNotEmpty()) {
            PostCount(
                label = post.likeLabel,
                description = stringResource(R.string.comments_like_count, post.likeLabel),
                icon = { Icon(Icons.Outlined.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
        if (post.commentLabel.isNotEmpty()) {
            PostCount(
                label = post.commentLabel,
                description = stringResource(R.string.channel_post_comment_count, post.commentLabel),
                icon = {
                    Icon(Icons.AutoMirrored.Outlined.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
        }
    }
}

@Composable
private fun PostCount(label: String, description: String, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

private fun postPublishedLabel(post: CommunityPost): String? =
    post.publishedAtMillis?.takeIf { it > 0 }?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } ?: post.publishedTime.takeIf(String::isNotBlank)

private const val POST_FOLD_CHARACTERS = 500
private const val POST_FOLD_LINES = 10
