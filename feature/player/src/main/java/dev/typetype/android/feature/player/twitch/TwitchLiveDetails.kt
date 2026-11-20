package dev.typetype.android.feature.player.twitch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.ProfileAvatar
import dev.typetype.android.domain.stream.Stream
import java.util.Locale
import kotlinx.coroutines.delay

// What sits between a Twitch live and its chat: the title, how many watch and for how long, the category, and the
// channel with its follow button. Kept short so the chat under it gets most of the screen.
@Composable
internal fun TwitchLiveDetails(stream: Stream, login: String, onOpenChannel: (String) -> Unit, modifier: Modifier) {
    val viewModel: TwitchLiveDetailsViewModel = hiltViewModel(key = "twitch-live-details")
    LaunchedEffect(login) { viewModel.start(login) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val info = state.channel?.liveInfo
    val startedAt = info?.startedAtMillis ?: stream.uploadedAtMillis
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = info?.title ?: stream.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.video_live_badge),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.background(LIVE_RED, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            val meta = listOfNotNull(
                stringResource(R.string.video_watching_short, compact(info?.viewers ?: stream.viewCount)),
                uptime(startedAt, now)?.let { stringResource(R.string.twitch_live_for, it) },
                info?.category,
            )
            Text(
                text = meta.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val name = state.channel?.displayName ?: stream.uploaderName
            Row(
                modifier = Modifier.weight(1f).clickable(role = Role.Button) { onOpenChannel(stream.uploaderUrl) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(
                    imageUrl = state.channel?.avatarUrl ?: stream.uploaderAvatarUrl,
                    fallbackLetter = name.take(1),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (state.followable) {
                val label = stringResource(if (state.following) R.string.twitch_channel_following else R.string.twitch_channel_follow)
                if (state.following) {
                    OutlinedButton(onClick = viewModel::toggleFollow) { Text(label) }
                } else {
                    Button(onClick = viewModel::toggleFollow) { Text(label) }
                }
            }
        }
    }
}

private fun compact(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(Locale.US, count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(Locale.US, count / 1_000.0)
    else -> count.coerceAtLeast(0).toString()
}.replace(".0", "")

private fun uptime(startedAtMillis: Long, now: Long): String? {
    if (startedAtMillis <= 0) return null
    val minutes = ((now - startedAtMillis) / 60_000).coerceAtLeast(0)
    return "%d:%02d".format(minutes / 60, minutes % 60)
}

private val LIVE_RED = Color(0xFFE91916)
private const val CLOCK_TICK_MS = 30_000L
