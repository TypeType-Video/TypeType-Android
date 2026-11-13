package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.rememberUserErrorMapper
import dev.typetype.android.domain.comments.CommentVoiceReply

@Composable
internal fun CommentVoiceReplyView(reply: CommentVoiceReply) {
    val playback = LocalVoiceReplyPlayback.current
    val playable = reply.playable && playback != null
    val state = if (playable) playback.stateOf(reply.url) else VoiceReplyState.Idle
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (playable) {
                VoiceReplyButton(state = state, onClick = { playback.toggle(reply.url) })
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (playable) 12.dp else 0.dp)) {
                VoiceReplyHeader(durationMs = reply.durationMs)
                if (playable) {
                    LinearProgressIndicator(
                        progress = { playback.progressOf(reply.url) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        drawStopIndicator = {},
                    )
                }
                if (state == VoiceReplyState.Error) {
                    Text(
                        text = rememberUserErrorMapper().message(playback?.failure, R.string.comments_voice_reply_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (reply.transcript.isNotBlank()) {
            Text(
                text = reply.transcript,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!reply.playable) reply.unavailableText?.let { UnavailableText(it) }
    }
}

@Composable
private fun VoiceReplyButton(state: VoiceReplyState, onClick: () -> Unit) {
    val playing = state == VoiceReplyState.Playing
    FilledIconButton(
        onClick = onClick,
        enabled = state != VoiceReplyState.Loading,
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
    ) {
        if (state == VoiceReplyState.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.comments_voice_reply_pause else R.string.comments_voice_reply_play,
                ),
            )
        }
    }
}

@Composable
private fun VoiceReplyHeader(durationMs: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.comments_voice_reply),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp).weight(1f),
        )
        if (durationMs > 0) {
            Text(
                text = formatVoiceReplyDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun formatVoiceReplyDuration(durationMs: Long): String {
    val seconds = (durationMs + 500) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
