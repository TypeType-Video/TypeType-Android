package dev.typetype.android.feature.settings.diagnostics

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.core.ui.copyPlainText

@Composable
internal fun SupportReportReceiptRow(reportId: String) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.support_report_receipt, reportId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                copyPlainText(
                    context = context,
                    value = reportId,
                    labelRes = R.string.support_report_clipboard_label,
                    confirmationRes = R.string.support_report_id_copied,
                )
            },
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.support_report_copy_id),
            )
        }
    }
}
