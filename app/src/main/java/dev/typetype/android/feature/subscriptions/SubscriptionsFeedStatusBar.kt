package dev.typetype.android.feature.subscriptions

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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow

@Composable
internal fun SubscriptionsFeedStatusBar(
    isRefreshing: Boolean,
    isServerRefreshing: Boolean,
    errorMessage: String?,
    requestId: String?,
    hasContent: Boolean,
    onRetry: () -> Unit,
) {
    when {
        errorMessage != null && hasContent -> FailureStatus(errorMessage, requestId, onRetry)
        isRefreshing && hasContent -> ProgressStatus(R.string.subscriptions_checking_updates)
        isServerRefreshing && hasContent -> ProgressStatus(R.string.subscriptions_rebuilding)
    }
}

@Composable
private fun ProgressStatus(label: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FailureStatus(message: String, requestId: String?, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                requestId?.let { RequestIdRow(requestId = it) }
            }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.state_retry))
            }
        }
    }
}
