package dev.typetype.android.feature.settings.rss

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R

@Composable
internal fun RssRegenerateDialog(
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RssConfirmationDialog(
        title = stringResource(R.string.rss_regenerate_title),
        message = stringResource(R.string.rss_regenerate_message),
        action = stringResource(R.string.rss_regenerate),
        enabled = enabled,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun RssDeleteDialog(
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RssConfirmationDialog(
        title = stringResource(R.string.rss_delete_title),
        message = stringResource(R.string.rss_delete_message),
        action = stringResource(R.string.rss_delete),
        enabled = enabled,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun RssConfirmationDialog(
    title: String,
    message: String,
    action: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(action) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
