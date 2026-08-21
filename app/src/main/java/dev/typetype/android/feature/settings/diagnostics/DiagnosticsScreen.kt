package dev.typetype.android.feature.settings.diagnostics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.copyPlainText
import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun DiagnosticsRoute(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DiagnosticsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onClear = viewModel::clear,
        onOpenReport = viewModel::openReportComposer,
        onCloseReport = viewModel::closeReportComposer,
        onSelectReportCategory = viewModel::selectReportCategory,
        onReportDescriptionChanged = viewModel::updateReportDescription,
        onRequestReportSubmit = viewModel::requestReportSubmission,
        onDismissReportSubmit = viewModel::dismissReportSubmission,
        onSubmitReport = viewModel::submitReport,
    )
}

@Composable
private fun DiagnosticsScreen(
    state: DiagnosticsState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onOpenReport: () -> Unit,
    onCloseReport: () -> Unit,
    onSelectReportCategory: (dev.typetype.android.domain.support.SupportReportCategory) -> Unit,
    onReportDescriptionChanged: (String) -> Unit,
    onRequestReportSubmit: () -> Unit,
    onDismissReportSubmit: () -> Unit,
    onSubmitReport: () -> Unit,
) {
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.diagnostics_share)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            DiagnosticsTopBar(onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { DiagnosticsExplanation() }
                state.crashReport?.let { report -> item { CrashDiagnosticCard(report) } }
                item {
                    DiagnosticsReportSection(
                        state = state,
                        onOpen = onOpenReport,
                        onClose = onCloseReport,
                        onSelectCategory = onSelectReportCategory,
                        onDescriptionChanged = onReportDescriptionChanged,
                        onRequestSubmit = onRequestReportSubmit,
                        onDismissSubmit = onDismissReportSubmit,
                        onSubmit = onSubmitReport,
                    )
                }
                item {
                    DiagnosticsActions(
                        hasEntries = state.entries.isNotEmpty() || state.crashReport != null,
                        onRefresh = onRefresh,
                        onClear = onClear,
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    buildDiagnosticReport(state.entries, state.crashReport),
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(intent, shareLabel),
                            )
                        },
                    )
                }
                when {
                    state.isLoading -> item { DiagnosticsLoading() }
                    state.entries.isEmpty() && state.crashReport == null -> item { DiagnosticsEmpty() }
                    else -> diagnosticRows(state.entries)
                }
            }
        }
    }
}

internal fun LazyListScope.diagnosticRows(entries: List<DiagnosticEntry>) {
    items(entries) { entry -> DiagnosticRow(entry) }
}

@Composable
private fun DiagnosticsExplanation() {
    Text(
        text = stringResource(R.string.diagnostics_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun DiagnosticsActions(
    hasEntries: Boolean,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.diagnostics_refresh))
        }
        OutlinedButton(onClick = onClear, enabled = hasEntries, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.diagnostics_clear))
        }
        Button(onClick = onShare, enabled = hasEntries, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.diagnostics_share))
        }
    }
}

@Composable
private fun DiagnosticsLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DiagnosticsEmpty() {
    Text(
        text = stringResource(R.string.diagnostics_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

@Composable
private fun DiagnosticRow(entry: DiagnosticEntry) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
    val context = LocalContext.current
    if (entry.method == "APP") {
        ApplicationDiagnosticRow(entry, formatter)
        return
    }
    val outcome = entry.statusCode?.let { stringResource(R.string.diagnostics_http_status, it) }
        ?: stringResource(R.string.diagnostics_network_error)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "${entry.method} ${entry.route}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = stringResource(R.string.diagnostics_result, outcome, entry.durationMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatter.format(Date(entry.timestampEpochMillis)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.sabr?.let {
            Text(
                text = stringResource(R.string.diagnostics_sabr_detail, it.redactedSummary()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        entry.failureCode?.let {
            Text(
                text = stringResource(R.string.diagnostics_failure_code, it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        entry.requestId?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.diagnostics_request_id, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        copyPlainText(
                            context = context,
                            value = it,
                            labelRes = R.string.diagnostics_clipboard_label,
                            confirmationRes = R.string.diagnostics_request_id_copied,
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.diagnostics_copy_request_id),
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationDiagnosticRow(entry: DiagnosticEntry, formatter: DateFormat) {
    val label = when (entry.route) {
        "/app/exit/anr" -> stringResource(R.string.diagnostics_event_anr)
        "/app/exit/low-memory" -> stringResource(R.string.diagnostics_event_low_memory)
        "/app/exit/user" -> stringResource(R.string.diagnostics_event_user_exit)
        "/app/exit/system" -> stringResource(R.string.diagnostics_event_system_exit)
        else -> stringResource(R.string.diagnostics_event_crash)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = formatter.format(Date(entry.timestampEpochMillis)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
