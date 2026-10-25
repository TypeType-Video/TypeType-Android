package dev.typetype.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.VideoRefusalReason

@Composable
internal fun HomeWelcomeCard(onEnable: () -> Unit, onLater: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text(stringResource(R.string.home_algo_welcome_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.home_algo_welcome_text), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEnable) { Text(stringResource(R.string.home_algo_welcome_enable)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.home_algo_welcome_later)) }
            }
        }
    }
}

@Composable
internal fun HomePreparingBanner() {
    Text(
        text = stringResource(R.string.home_algo_preparing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
internal fun HomeRowHeader(title: String, reason: String) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(reasonLabel(reason)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NotForMeButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Outlined.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.home_algo_not_for_me),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RefusedVideoCard(
    refusal: VideoRefusal,
    onExplain: (VideoRefusalReason) -> Unit,
    onUndo: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.home_algo_removed), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.home_algo_why),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoRefusalReason.entries.forEach { reason ->
                    FilterChip(
                        selected = refusal.reason == reason,
                        onClick = { onExplain(reason) },
                        label = { Text(stringResource(reason.label())) },
                    )
                }
            }
            TextButton(onClick = onUndo) { Text(stringResource(R.string.home_algo_undo)) }
        }
    }
}

private fun VideoRefusalReason.label(): Int = when (this) {
    VideoRefusalReason.Seen -> R.string.home_algo_reason_seen
    VideoRefusalReason.Channel -> R.string.home_algo_reason_channel
    VideoRefusalReason.Topic -> R.string.home_algo_reason_topic
    VideoRefusalReason.Other -> R.string.home_algo_reason_other
}

internal fun reasonLabel(reason: String): Int = when (reason) {
    "watched", "same_channel" -> R.string.algo_part_recent
    "continue" -> R.string.algo_part_continue
    "watch_again" -> R.string.algo_part_watch_again
    "subscriptions", "new_from_subscriptions" -> R.string.algo_part_subscriptions
    "topic" -> R.string.algo_part_topics
    "discovery" -> R.string.algo_part_discovery
    "trending" -> R.string.algo_part_trending
    else -> R.string.algo_part_recommended
}
