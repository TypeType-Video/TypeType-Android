package dev.typetype.android.feature.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl
import dev.typetype.android.domain.channel.Channel

private val BannerAspectRatio = 16f / 6f
private val AvatarSize = 64.dp

@Composable
internal fun ChannelHeader(
    channel: Channel,
    isSubscribed: Boolean,
    subscribeInFlight: Boolean,
    onToggleSubscribe: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (channel.bannerUrl.isNullOrBlank()) {
            ChannelBackButton(onNavigateBack = onNavigateBack)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(BannerAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = buildImageUrl(serverBaseUrl, channel.bannerUrl.orEmpty()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                ChannelBackButton(onNavigateBack = onNavigateBack)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = buildImageUrl(serverBaseUrl, channel.avatarUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(modifier = Modifier.weight(1f)) {
                ChannelName(channel)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatSubscribers(channel.subscriberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SubscribeButton(
                isSubscribed = isSubscribed,
                enabled = !subscribeInFlight,
                onClick = onToggleSubscribe,
            )
        }
    }
}

@Composable
private fun ChannelBackButton(onNavigateBack: () -> Unit) {
    IconButton(
        onClick = onNavigateBack,
        modifier = Modifier.padding(8.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.settings_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ChannelName(channel: Channel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = channel.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (channel.verified) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SubscribeButton(isSubscribed: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSubscribed) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(34.dp),
    ) {
        Text(
            text = stringResource(
                if (isSubscribed) R.string.channel_subscribed else R.string.channel_subscribe,
            ),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun formatSubscribers(count: Long): String = when {
    count >= 1_000_000 -> stringResource(R.string.channel_subscribers_short_million, count / 1_000_000.0)
    count >= 1_000 -> stringResource(R.string.channel_subscribers_short_thousand, count / 1_000.0)
    else -> pluralStringResource(
        R.plurals.channel_subscribers_count,
        count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        count,
    )
}
