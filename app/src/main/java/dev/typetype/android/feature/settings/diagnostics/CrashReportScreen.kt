package dev.typetype.android.feature.settings.diagnostics

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.typetype.android.R
import dev.typetype.android.core.ui.copyPlainText
import dev.typetype.android.domain.diagnostics.CrashReport
import java.text.DateFormat
import java.util.Date

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CrashReportRoute(
    report: CrashReport,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    var comment by rememberSaveable(report.fingerprint) { mutableStateOf("") }
    var linkFailure by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onContinue)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            TopAppBar(title = { Text(stringResource(R.string.crash_report_title)) })
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { CrashReportIntroduction() }
                item { CrashMetadata(report) }
                if (report.lastRequest != null || report.lastSabrSummary != null) {
                    item { CrashRequestDetails(report) }
                }
                item { CrashTrace(report.stackTrace) }
                item {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
                        label = { Text(stringResource(R.string.crash_report_comment_label)) },
                        supportingText = {
                            Text(stringResource(R.string.crash_report_comment_help))
                        },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CrashReportActions(
                        onCopy = {
                            copyPlainText(
                                context = context,
                                value = buildCrashReportMarkdown(report, comment),
                                labelRes = R.string.crash_report_clipboard_label,
                                confirmationRes = R.string.crash_report_copied,
                            )
                        },
                        onOpenIssues = {
                            linkFailure = runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, ISSUES_URL.toUri()))
                            }.isFailure
                        },
                        onContinue = onContinue,
                    )
                }
                if (linkFailure) {
                    item {
                        Text(
                            text = stringResource(R.string.crash_report_open_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashReportIntroduction() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.crash_report_sorry),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.crash_report_explanation),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.crash_report_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CrashMetadata(report: CrashReport) {
    val occurredAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        .format(Date(report.occurredAtEpochMillis))
    CrashSection(title = stringResource(R.string.crash_report_details)) {
        MetadataRow(
            stringResource(R.string.crash_report_action),
            stringResource(R.string.crash_report_action_crash),
        )
        MetadataRow(stringResource(R.string.crash_report_time), occurredAt)
        MetadataRow(
            stringResource(R.string.crash_report_app_version),
            "${report.appVersion} (${report.appVersionCode})",
        )
        MetadataRow(
            stringResource(R.string.crash_report_android_version),
            "${report.androidVersion} (API ${report.apiLevel})",
        )
        MetadataRow(
            stringResource(R.string.crash_report_device),
            "${report.deviceManufacturer} ${report.deviceModel}",
        )
        MetadataRow(stringResource(R.string.crash_report_exception), report.exceptionType)
        MetadataRow(stringResource(R.string.crash_report_fingerprint), report.fingerprint)
    }
}

@Composable
private fun CrashRequestDetails(report: CrashReport) {
    CrashSection(title = stringResource(R.string.crash_report_context)) {
        report.lastRequest?.let { request ->
            MetadataRow(
                stringResource(R.string.crash_report_last_request),
                "${request.method} ${request.route}",
            )
            request.requestId?.let {
                MetadataRow(stringResource(R.string.crash_report_request_id), it)
            }
        }
        report.lastSabrSummary?.let {
            MetadataRow(stringResource(R.string.crash_report_last_sabr), it)
        }
    }
}

@Composable
private fun CrashTrace(lines: List<String>) {
    CrashSection(title = stringResource(R.string.crash_report_stack_trace)) {
        SelectionContainer {
            Text(
                text = lines.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun CrashSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f),
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.62f),
            )
        }
    }
}

@Composable
private fun CrashReportActions(
    onCopy: () -> Unit,
    onOpenIssues: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.crash_report_copy))
        }
        OutlinedButton(onClick = onOpenIssues, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.crash_report_open_github))
        }
        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.crash_report_continue))
        }
    }
}

private const val MAX_COMMENT_LENGTH = 4_000
private const val ISSUES_URL = "https://github.com/TypeType-Video/TypeType-Android/issues"
