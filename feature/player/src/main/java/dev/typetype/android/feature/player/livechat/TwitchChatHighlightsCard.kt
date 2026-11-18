package dev.typetype.android.feature.player.livechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchPinnedMessage
import dev.typetype.android.domain.twitch.TwitchPoll

@Composable
internal fun TwitchChatHighlightsCards(
    highlights: TwitchChatHighlights,
    onHide: (String) -> Unit,
    onVote: ((pollId: String, choiceId: String) -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        highlights.pinned?.let { PinnedCard(it, onHide = { onHide(it.id) }) }
        highlights.poll?.let { poll -> PollCard(poll, onHide = { onHide(poll.id) }, onVote = onVote?.let { vote -> { choice -> vote(poll.id, choice) } }) }
    }
}

@Composable
private fun HighlightCard(onHide: () -> Unit, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp)) { content() }
            IconButton(onClick = onHide) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.live_chat_hide_highlight))
            }
        }
    }
}

@Composable
private fun PinnedCard(pinned: TwitchPinnedMessage, onHide: () -> Unit) {
    HighlightCard(onHide) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.live_chat_pinned_by, pinned.author),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(pinned.text, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PollCard(poll: TwitchPoll, onHide: () -> Unit, onVote: ((String) -> Unit)?) {
    val total = poll.choices.sumOf { it.votes }
    val best = poll.choices.maxOfOrNull { it.votes } ?: 0
    HighlightCard(onHide) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(poll.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            poll.choices.forEach { choice ->
                val percent = if (total > 0) Math.round(choice.votes * 100.0 / total).toInt() else 0
                val label = stringResource(R.string.live_chat_poll_choice, choice.title, percent)
                PollRow(label, percent, leading = best > 0 && choice.votes == best, onVote = onVote?.let { { it(choice.id) } })
            }
            Text(
                pluralStringResource(R.plurals.live_chat_poll_votes, poll.votes.toInt().coerceAtLeast(0), poll.votes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PollRow(label: String, percent: Int, leading: Boolean, onVote: (() -> Unit)?) {
    val fill = if (leading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val voteLabel = stringResource(R.string.live_chat_vote, label)
    val semantics = Modifier.clearAndSetSemantics { contentDescription = if (onVote != null) voteLabel else label }
    Column(if (onVote != null) semantics.clickable(onClickLabel = voteLabel, onClick = onVote) else semantics) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(fill))
        }
    }
}
