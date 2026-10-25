package dev.typetype.android.feature.settings.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.support.SupportReportCategory

@Composable
internal fun DiagnosticsReportSection(
    state: DiagnosticsState,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelectCategory: (SupportReportCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRequestSubmit: () -> Unit,
    onDismissSubmit: () -> Unit,
    onSubmit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportHeading()
            when {
                state.reportReceipt != null -> ReportSuccess(state)
                !state.reportAvailabilityLoaded -> CircularProgressIndicator()
                !state.canSubmitReport -> ReportUnavailable()
                !state.isReportComposerVisible -> ReportIntroduction(onOpen)
                else -> ReportComposer(
                    state = state,
                    onClose = onClose,
                    onSelectCategory = onSelectCategory,
                    onDescriptionChanged = onDescriptionChanged,
                    onRequestSubmit = onRequestSubmit,
                )
            }
        }
    }
    if (state.isSubmitConfirmationVisible) {
        ReportConfirmationDialog(
            state = state,
            onDismiss = onDismissSubmit,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun ReportHeading() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
        Column {
            Text(
                text = stringResource(R.string.support_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.support_report_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportIntroduction(onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.support_report_privacy_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpen) {
            Text(stringResource(R.string.support_report_create))
        }
    }
}

@Composable
private fun ReportUnavailable() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null)
        Text(
            text = stringResource(R.string.support_report_member_required),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportSuccess(state: DiagnosticsState) {
    val receipt = requireNotNull(state.reportReceipt)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.support_report_sent),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            SupportReportReceiptRow(reportId = receipt.id)
        }
    }
}

@Composable
private fun ReportComposer(
    state: DiagnosticsState,
    onClose: () -> Unit,
    onSelectCategory: (SupportReportCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRequestSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.support_report_category),
            style = MaterialTheme.typography.labelLarge,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SupportReportCategory.entries) { category ->
                FilterChip(
                    selected = state.reportCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category.label()) },
                )
            }
        }
        OutlinedTextField(
            value = state.reportDescription,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(R.string.support_report_description)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.support_report_description_count,
                        state.reportDescription.length,
                        MAX_DESCRIPTION_LENGTH,
                    ),
                )
            },
            minLines = 4,
            maxLines = 8,
            isError = state.reportErrorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = attachmentSummary(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.support_report_review_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.reportErrorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            state.reportErrorRequestId?.let { RequestIdRow(requestId = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClose,
                enabled = !state.isSubmittingReport,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onRequestSubmit,
                enabled = !state.isSubmittingReport && state.reportDescription.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSubmittingReport) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.support_report_review))
                }
            }
        }
    }
}

@Composable
private fun ReportConfirmationDialog(
    state: DiagnosticsState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_report_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.reportDescription.trim())
                Text(
                    text = attachmentSummary(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.support_report_confirm_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit) {
                Text(stringResource(R.string.support_report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun attachmentSummary(state: DiagnosticsState): String {
    val errors = state.entries.count { it.method != "APP" && (it.statusCode ?: 0) >= 400 }
        .coerceAtMost(MAX_API_ERRORS)
    val events = state.entries.count { it.method == "APP" }.coerceAtMost(MAX_CRASH_LOGS)
    return stringResource(R.string.support_report_attachment_summary, errors, events)
}

@Composable
private fun SupportReportCategory.label(): String = stringResource(
    when (this) {
        SupportReportCategory.Player -> R.string.support_report_category_player
        SupportReportCategory.AudioLanguage -> R.string.support_report_category_audio
        SupportReportCategory.Subtitles -> R.string.support_report_category_subtitles
        SupportReportCategory.Interface -> R.string.support_report_category_interface
        SupportReportCategory.Functionality -> R.string.support_report_category_functionality
    },
)

private const val MAX_DESCRIPTION_LENGTH = 10_000
private const val MAX_CRASH_LOGS = 200
private const val MAX_API_ERRORS = 100
