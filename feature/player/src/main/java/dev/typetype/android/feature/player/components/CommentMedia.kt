package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.url.buildImageUrl
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentImage

@Composable
internal fun CommentMedia(comment: Comment, modifier: Modifier = Modifier) {
    val image = comment.image
    val voiceReply = comment.voiceReply
    if (image == null && voiceReply == null) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        image?.let { CommentImageView(it) }
        voiceReply?.let { CommentVoiceReplyView(it) }
    }
}

@Composable
private fun CommentImageView(image: CommentImage) {
    val url = image.url
    if (url == null) {
        image.unavailableText?.let { UnavailableText(it) }
        return
    }
    AsyncImage(
        model = buildImageUrl(LocalServerBaseUrl.current, url),
        contentDescription = stringResource(
            if (image.animated) R.string.comments_gif else R.string.comments_image,
        ),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .widthIn(max = 280.dp)
            .heightIn(max = 240.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
internal fun UnavailableText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
