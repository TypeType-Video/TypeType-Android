package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video

@Composable
internal fun DetailsChannelRow(
    video: Video,
    stream: StreamDetails?,
    isAuthenticated: Boolean,
    isSubscribed: Boolean,
    isActionInProgress: Boolean,
    onOpenChannel: () -> Unit,
    onToggleSubscription: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onOpenChannel,
            enabled = stream?.uploaderUrl?.isNotBlank() == true,
            colors = cinematicButtonColors(),
        ) {
            val avatar = stream?.uploaderAvatarUrl?.takeIf(String::isNotBlank) ?: video.uploaderAvatarUrl
            if (avatar.isNotBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = video.uploaderName,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(9.dp))
            }
            Text(video.uploaderName, fontWeight = FontWeight.SemiBold)
            stream?.uploaderSubscriberCount?.takeIf { it > 0L }?.let {
                Spacer(Modifier.width(9.dp))
                Text(
                    compactSubscribers(it),
                    modifier = Modifier.alpha(.62f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (isAuthenticated && stream?.uploaderUrl?.isNotBlank() == true) {
            Button(
                onClick = onToggleSubscription,
                enabled = !isActionInProgress,
                colors = cinematicButtonColors(),
            ) {
                Icon(
                    if (isSubscribed) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = null,
                )
                Spacer(Modifier.width(7.dp))
                Text(if (isSubscribed) "Subscribed" else "Subscribe")
            }
        }
    }
}

@Composable
private fun cinematicButtonColors() = ButtonDefaults.colors(
    containerColor = Color.White.copy(alpha = .14f),
    contentColor = Color.White,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = Color.White.copy(alpha = .12f),
    disabledContentColor = Color.White.copy(alpha = .64f),
)

private fun compactSubscribers(value: Long): String = when {
    value >= 1_000_000L -> "%.1fM subscribers".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK subscribers".format(value / 1_000.0)
    else -> "$value subscribers"
}
