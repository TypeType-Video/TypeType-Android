package dev.typetype.android.feature.settings.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.download.DownloadFailure
import dev.typetype.android.domain.download.DownloadItem
import dev.typetype.android.domain.download.DownloadMediaMode
import dev.typetype.android.domain.download.DownloadStage
import dev.typetype.android.domain.download.DownloadStatus

@Composable
fun StorageDownloadRow(
    item: DownloadItem,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = item.status == DownloadStatus.Successful, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.statusLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.selectionLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (item.status == DownloadStatus.Pending || item.status == DownloadStatus.Running) {
            LinearProgressIndicator(
                progress = { (item.progressPercent ?: 4).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DownloadActions(item.status, onOpen, onCancel, onRetry, onRemove)
    }
}

@Composable
private fun DownloadActions(
    status: DownloadStatus,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        when (status) {
            DownloadStatus.Pending, DownloadStatus.Running -> {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.download_action_cancel)) }
            }
            DownloadStatus.Successful -> {
                TextButton(onClick = onOpen) { Text(stringResource(R.string.download_action_open)) }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.download_action_remove)) }
            }
            DownloadStatus.Failed, DownloadStatus.Cancelled -> {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.download_action_retry)) }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.download_action_remove)) }
            }
        }
    }
}

@Composable
private fun DownloadItem.selectionLabel(): String = when (selection.mode) {
    DownloadMediaMode.Audio -> stringResource(R.string.download_item_audio_only)
    DownloadMediaMode.Video -> stringResource(
        R.string.download_item_video_quality,
        requireNotNull(selection.maxHeight),
    )
}

@Composable
private fun DownloadItem.statusLabel(): String = when (status) {
    DownloadStatus.Pending -> stringResource(R.string.download_status_pending)
    DownloadStatus.Successful -> stringResource(R.string.download_status_successful)
    DownloadStatus.Cancelled -> stringResource(R.string.download_status_cancelled)
    DownloadStatus.Failed -> failure.failureLabel()
    DownloadStatus.Running -> when (stage) {
        DownloadStage.Downloading -> progressPercent?.let {
            stringResource(R.string.download_status_running_percent, it)
        } ?: stringResource(R.string.download_status_running)
        DownloadStage.Finalizing -> stringResource(R.string.download_status_finalizing)
        DownloadStage.Preparing, null -> stringResource(R.string.download_status_preparing)
    }
}

@Composable
private fun DownloadFailure?.failureLabel(): String = when (this) {
    DownloadFailure.Authentication -> stringResource(R.string.download_failure_authentication)
    DownloadFailure.InsufficientStorage -> stringResource(R.string.download_failure_storage)
    DownloadFailure.Network -> stringResource(R.string.download_failure_network)
    DownloadFailure.Rejected -> stringResource(R.string.download_failure_rejected)
    DownloadFailure.ServerUnavailable -> stringResource(R.string.download_failure_server)
    DownloadFailure.TimedOut -> stringResource(R.string.download_failure_timeout)
    DownloadFailure.Unknown, null -> stringResource(R.string.download_status_failed)
}
