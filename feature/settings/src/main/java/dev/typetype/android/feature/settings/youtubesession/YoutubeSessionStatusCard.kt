package dev.typetype.android.feature.settings.youtubesession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import java.text.DateFormat
import java.util.Date

@Composable
internal fun YoutubeSessionStatusCard(
    state: YoutubeSessionState,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDisconnect by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.youtube_session_status_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.isStatusLoading && state.session == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(3.dp))
                    Text(stringResource(R.string.youtube_session_status_loading))
                }
            } else {
                Text(
                    text = statusLabel(state.session?.status),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = statusDescription(state.session?.status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusTime(
                    label = stringResource(R.string.youtube_session_last_used),
                    timestamp = state.session?.lastUsedAt,
                )
                StatusTime(
                    label = stringResource(R.string.youtube_session_updated),
                    timestamp = state.session?.updatedAt,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.isStatusLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.youtube_session_retry_status))
                }
                Button(
                    onClick = { confirmDisconnect = true },
                    enabled = state.canDisconnect,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.youtube_session_disconnect))
                }
            }
        }
    }
    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.youtube_session_disconnect_title)) },
            text = { Text(stringResource(R.string.youtube_session_disconnect_description)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    onDisconnect()
                }) {
                    Text(stringResource(R.string.youtube_session_disconnect))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusTime(label: String, timestamp: Long?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatSessionTime(timestamp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun statusLabel(status: YoutubeSessionStatus?): String = stringResource(
    when (status) {
        YoutubeSessionStatus.Connected -> R.string.youtube_session_status_connected
        YoutubeSessionStatus.NeedsReconnect -> R.string.youtube_session_status_reconnect
        YoutubeSessionStatus.Unknown -> R.string.youtube_session_status_unknown
        YoutubeSessionStatus.Disconnected, null -> R.string.youtube_session_status_disconnected
    },
)

@Composable
private fun statusDescription(status: YoutubeSessionStatus?): String = stringResource(
    when (status) {
        YoutubeSessionStatus.Connected -> R.string.youtube_session_status_connected_description
        YoutubeSessionStatus.NeedsReconnect -> R.string.youtube_session_status_reconnect_description
        YoutubeSessionStatus.Unknown -> R.string.youtube_session_status_unknown_description
        YoutubeSessionStatus.Disconnected, null -> R.string.youtube_session_status_disconnected_description
    },
)

@Composable
private fun formatSessionTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return stringResource(R.string.youtube_session_never)
    return remember(timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    }
}
