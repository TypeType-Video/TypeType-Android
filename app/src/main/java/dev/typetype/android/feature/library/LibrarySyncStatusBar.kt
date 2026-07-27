package dev.typetype.android.feature.library

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow

@Composable
fun LibrarySyncStatusBar(
    isRefreshing: Boolean,
    lastSuccessfulSyncAtMillis: Long?,
    errorMessage: String?,
    requestId: String?,
    pendingWriteCount: Int,
    failedWriteCount: Int,
    onRetry: () -> Unit,
) {
    when {
        isRefreshing -> RefreshingStatus()
        failedWriteCount > 0 && errorMessage != null -> FailureStatus(errorMessage, requestId, onRetry)
        pendingWriteCount > 0 -> PendingWritesStatus(pendingWriteCount)
        errorMessage != null -> FailureStatus(errorMessage, requestId, onRetry)
        lastSuccessfulSyncAtMillis != null -> LastSuccessStatus(lastSuccessfulSyncAtMillis)
    }
}

@Composable
private fun PendingWritesStatus(count: Int) {
    Text(
        text = pluralStringResource(R.plurals.library_changes_syncing, count, count),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun RefreshingStatus() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.library_refreshing),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FailureStatus(errorMessage: String, requestId: String?, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
                requestId?.let { RequestIdRow(requestId = it) }
            }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.state_retry))
            }
        }
    }
}

@Composable
private fun LastSuccessStatus(timestamp: Long) {
    val relative = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    Text(
        text = stringResource(R.string.library_last_synced, relative),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
