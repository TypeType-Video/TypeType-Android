package dev.typetype.android.feature.settings.imports

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.domain.imports.YoutubeTakeoutCategoryCounts
import dev.typetype.android.domain.imports.YoutubeTakeoutImportItem
import dev.typetype.android.domain.imports.YoutubeTakeoutImportStatus

@Composable
internal fun YoutubeTakeoutImportSection(
    state: YoutubeTakeoutImportState,
    onOpenTakeout: () -> Unit,
    onChooseArchives: () -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRetryCollectionRefresh: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_import_youtube_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
        text = stringResource(R.string.settings_import_youtube_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TypeTypeCard {
        TypeTypeSecondaryButton(
            text = stringResource(R.string.settings_import_youtube_open_takeout),
            onClick = onOpenTakeout,
        )
        TypeTypePrimaryButton(
            text = stringResource(R.string.settings_import_youtube_choose_files),
            onClick = onChooseArchives,
            enabled = !state.isReadingDocuments,
            isLoading = state.isReadingDocuments,
        )
        Text(
            text = stringResource(R.string.settings_import_youtube_background),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.items.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_import_youtube_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        state.items.forEach { item ->
            YoutubeTakeoutImportCard(item, onRetry, onCancel, onRemove)
        }
    }
    youtubeTakeoutErrorMessage(state.errorKey)?.let { message ->
        TypeTypeCard {
            Text(message, color = MaterialTheme.colorScheme.error)
            state.errorRequestId?.let { RequestIdRow(requestId = it) }
            if (state.errorKey == "YOUTUBE_IMPORT_REFRESH_FAILED") {
                TypeTypeSecondaryButton(
                    text = stringResource(R.string.settings_import_youtube_retry_refresh),
                    onClick = onRetryCollectionRefresh,
                )
            }
        }
    }
}

@Composable
private fun YoutubeTakeoutImportCard(
    item: YoutubeTakeoutImportItem,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    TypeTypeCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.sizeBytes?.let { size ->
                    Text(
                        text = Formatter.formatShortFileSize(context, size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(item.status.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = item.status.statusColor(),
            )
        }
        if (item.status.isActive()) {
            LinearProgressIndicator(
                progress = { (item.progressPercent ?: 2).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item.preview?.let { PreviewSummary(it) }
        if (item.status == YoutubeTakeoutImportStatus.Completed) {
            CompletionSummary(item)
        }
        if (item.warningCount > 0 || item.errorCount > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryValueRow(R.string.settings_import_youtube_warnings, item.warningCount)
            SummaryValueRow(R.string.settings_import_youtube_errors, item.errorCount)
        }
        youtubeTakeoutErrorMessage(item.failureCode)?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        item.failureRequestId?.let { RequestIdRow(requestId = it) }
        ImportActions(item, onRetry, onCancel, onRemove)
    }
}

@Composable
private fun PreviewSummary(counts: YoutubeTakeoutCategoryCounts) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text(
        text = stringResource(R.string.settings_import_youtube_preview),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SummaryValueRow(R.string.settings_import_subscriptions, counts.subscriptions)
    SummaryValueRow(R.string.settings_import_playlists, counts.playlists)
    SummaryValueRow(R.string.settings_import_playlist_videos, counts.playlistItems)
    SummaryValueRow(R.string.settings_import_youtube_favorites, counts.favorites)
    SummaryValueRow(R.string.settings_import_youtube_watch_later, counts.watchLater)
    SummaryValueRow(R.string.settings_import_history, counts.history)
}

@Composable
private fun CompletionSummary(item: YoutubeTakeoutImportItem) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    SummaryValueRow(R.string.settings_import_youtube_imported, item.importedCount ?: 0)
    SummaryValueRow(R.string.settings_import_youtube_skipped, item.skippedCount ?: 0)
    SummaryValueRow(R.string.settings_import_youtube_failed_items, item.failedCount ?: 0)
}

@Composable
private fun SummaryValueRow(labelRes: Int, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value.toString(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ImportActions(
    item: YoutubeTakeoutImportItem,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        when (item.status) {
            YoutubeTakeoutImportStatus.Queued,
            YoutubeTakeoutImportStatus.Uploading,
            YoutubeTakeoutImportStatus.Parsing,
            YoutubeTakeoutImportStatus.Importing,
            -> TextButton(onClick = { onCancel(item.requestId) }) {
                Text(stringResource(R.string.settings_import_youtube_cancel))
            }
            YoutubeTakeoutImportStatus.Failed,
            YoutubeTakeoutImportStatus.Cancelled,
            -> {
                TextButton(onClick = { onRetry(item.requestId) }) {
                    Text(stringResource(R.string.settings_import_youtube_retry))
                }
                TextButton(onClick = { onRemove(item.requestId) }) {
                    Text(stringResource(R.string.settings_import_youtube_remove))
                }
            }
            YoutubeTakeoutImportStatus.Completed -> TextButton(onClick = { onRemove(item.requestId) }) {
                Text(stringResource(R.string.settings_import_youtube_remove))
            }
        }
    }
}

private fun YoutubeTakeoutImportStatus.labelRes(): Int = when (this) {
    YoutubeTakeoutImportStatus.Queued -> R.string.settings_import_youtube_queued
    YoutubeTakeoutImportStatus.Uploading -> R.string.settings_import_youtube_uploading
    YoutubeTakeoutImportStatus.Parsing -> R.string.settings_import_youtube_parsing
    YoutubeTakeoutImportStatus.Importing -> R.string.settings_import_youtube_importing
    YoutubeTakeoutImportStatus.Completed -> R.string.settings_import_youtube_completed
    YoutubeTakeoutImportStatus.Failed -> R.string.settings_import_youtube_failed
    YoutubeTakeoutImportStatus.Cancelled -> R.string.settings_import_youtube_cancelled
}

private fun YoutubeTakeoutImportStatus.isActive(): Boolean = this in setOf(
    YoutubeTakeoutImportStatus.Queued,
    YoutubeTakeoutImportStatus.Uploading,
    YoutubeTakeoutImportStatus.Parsing,
    YoutubeTakeoutImportStatus.Importing,
)

@Composable
private fun YoutubeTakeoutImportStatus.statusColor() = when (this) {
    YoutubeTakeoutImportStatus.Completed -> MaterialTheme.colorScheme.primary
    YoutubeTakeoutImportStatus.Failed -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun youtubeTakeoutErrorMessage(key: String?): String? = when (key) {
    null, "YOUTUBE_IMPORT_CANCELLED" -> null
    "YOUTUBE_IMPORT_INVALID_ARCHIVE" -> stringResource(R.string.settings_import_youtube_invalid_archive)
    "YOUTUBE_IMPORT_TOO_LARGE" -> stringResource(R.string.settings_import_youtube_too_large)
    "YOUTUBE_IMPORT_ACCOUNT_REQUIRED", "YOUTUBE_IMPORT_AUTHENTICATION" ->
        stringResource(R.string.settings_import_youtube_account_required)
    "YOUTUBE_IMPORT_PERMISSION" -> stringResource(R.string.settings_import_youtube_permission)
    "YOUTUBE_IMPORT_SCOPE_CHANGED" -> stringResource(R.string.settings_import_youtube_scope_changed)
    "YOUTUBE_IMPORT_JOB_NOT_FOUND" -> stringResource(R.string.settings_import_youtube_job_missing)
    "YOUTUBE_IMPORT_NETWORK" -> stringResource(R.string.settings_import_youtube_network)
    "YOUTUBE_IMPORT_SERVER_UNAVAILABLE" -> stringResource(R.string.settings_import_youtube_server_unavailable)
    "YOUTUBE_IMPORT_TIMED_OUT" -> stringResource(R.string.settings_import_youtube_timed_out)
    "YOUTUBE_IMPORT_UNSUPPORTED" -> stringResource(R.string.settings_import_youtube_unsupported)
    "YOUTUBE_IMPORT_REFRESH_FAILED" -> stringResource(R.string.settings_import_youtube_refresh_failed)
    else -> stringResource(R.string.settings_import_youtube_unknown_error)
}
