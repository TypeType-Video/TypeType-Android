package dev.typetype.android.feature.shorts

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.feed.Video

@Composable
internal fun ShortsInfoOverlay(
    video: Video,
    title: String,
    stats: ShortsVideoStats,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    onOpenChannel: () -> Unit,
    onCopyTitle: (String) -> Unit,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val titleCopyEnabled = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val copyTitleLabel = stringResource(R.string.shorts_copy_title)
    val copyTitle = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onCopyTitle(title)
    }
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 80.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onOpenChannel)
                    .padding(vertical = 6.dp),
            ) {
                AsyncImage(
                    model = video.uploaderAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = video.uploaderName,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (video.uploaderUrl.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onToggleSubscription,
                    enabled = !subscriptionInFlight,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) {
                            Color.White.copy(alpha = 0.18f)
                        } else {
                            Color.White
                        },
                        contentColor = if (isSubscribed) Color.White else Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.18f),
                        disabledContentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    if (subscriptionInFlight) {
                        CircularProgressIndicator(
                            color = if (isSubscribed) Color.White else Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (isSubscribed) R.string.channel_subscribed
                                else R.string.channel_subscribe,
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = if (titleCopyEnabled) {
                Modifier
                    .pointerInput(title, onCopyTitle) {
                        detectTapGestures(onLongPress = { copyTitle() })
                    }
                    .semantics {
                        onLongClick(copyTitleLabel) {
                            copyTitle()
                            true
                        }
                    }
            } else {
                Modifier
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stats.viewCount?.let { views ->
                ShortsStat(
                    value = formatShortsCount(views),
                    icon = Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        R.string.player_views_count,
                        formatShortsCount(views),
                    ),
                )
            }
            stats.likeCount?.let { likes ->
                ShortsStat(
                    value = formatShortsCount(likes),
                    icon = Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(
                        R.string.player_likes_count,
                        formatShortsCount(likes),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ShortsStat(value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(17.dp),
        )
        Text(text = value, color = Color.White.copy(alpha = 0.88f))
    }
}

internal fun formatShortsCount(value: Long): String = when {
    value >= 1_000_000_000L -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.coerceAtLeast(0L).toString()
}
