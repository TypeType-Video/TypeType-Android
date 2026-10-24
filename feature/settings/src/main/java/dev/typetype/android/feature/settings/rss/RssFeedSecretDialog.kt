package dev.typetype.android.feature.settings.rss

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.copyPlainText

@Composable
internal fun RssFeedSecretDialog(
    feedName: String,
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.rss_share)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rss_secret_title_named, feedName)) },
        text = {
            Column {
                Text(stringResource(R.string.rss_secret_notice))
                Text(
                    text = url,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    copyPlainText(
                        context,
                        url,
                        R.string.rss_clipboard_label,
                        R.string.rss_copied,
                    )
                },
            ) {
                Text(stringResource(R.string.rss_copy))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, shareTitle),
                    )
                },
            ) {
                Text(stringResource(R.string.rss_share))
            }
        },
    )
}
