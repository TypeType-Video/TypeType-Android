package dev.typetype.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.domain.library.HistoryItem

@Composable
internal fun ContinueWatchingSection(
    items: List<HistoryItem>,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            text = stringResource(R.string.home_section_continue_watching),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = items,
                key = { "continue-${it.id}" },
                contentType = { "continue-watching-video" },
            ) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onPlayVideo(item.url) },
                    onOpenChannel = onOpenChannel,
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    val branding = rememberVideoBranding(
        sourceUrl = item.url,
        title = item.title,
        thumbnailUrl = item.thumbnailUrl,
        durationSeconds = item.durationSeconds,
    )
    Column(
        modifier = Modifier
            .width(224.dp)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = branding.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            DurationBadge(
                durationSeconds = item.durationSeconds,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            )
            WatchProgressBar(
                progressSeconds = item.progressSeconds,
                durationSeconds = item.durationSeconds,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = branding.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.channelAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = item.channelAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (item.channelUrl.isNotBlank()) {
                                Modifier.clickable(role = Role.Button) {
                                    onOpenChannel(item.channelUrl)
                                }
                            } else {
                                Modifier
                            },
                        ),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = item.channelName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (item.channelUrl.isNotBlank()) {
                    Modifier.clickable(role = Role.Button) { onOpenChannel(item.channelUrl) }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun DurationBadge(durationSeconds: Long, modifier: Modifier = Modifier) {
    if (durationSeconds <= 0L) return
    Text(
        text = formatDuration(durationSeconds),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun WatchProgressBar(
    progressSeconds: Long,
    durationSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val fraction = if (durationSeconds > 0L) {
        progressSeconds / durationSeconds.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainder)
    } else {
        "%d:%02d".format(minutes, remainder)
    }
}
