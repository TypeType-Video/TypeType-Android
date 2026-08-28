package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.imports.PortabilityJob
import dev.typetype.android.domain.imports.PortabilityJobState

@Composable
internal fun PortabilityJobCard(
    job: PortabilityJob,
    isCancelling: Boolean,
    onCancel: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val active = job.state in setOf(
        PortabilityJobState.Queued,
        PortabilityJobState.Analyzing,
        PortabilityJobState.Applying,
        PortabilityJobState.Encoding,
    )
    val percent = job.progress?.total?.takeIf { it > 0 }?.let { total ->
        (job.progress.processed * 100f / total).toInt().coerceIn(0, 100)
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    active -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    job.state == PortabilityJobState.Failed -> Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    else -> Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.stateLabel(), style = MaterialTheme.typography.bodyMedium)
                    job.progress?.let { progress ->
                        Text(
                            text = buildString {
                                append(progress.processed)
                                progress.total?.let { append(" / $it") }
                                append(" ${progress.unit.orEmpty()}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (job.state == PortabilityJobState.Failed) {
                        Text(
                            text = job.errorMessage
                                ?: stringResource(R.string.portability_operation_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (active) {
                    IconButton(onClick = onCancel, enabled = !isCancelling) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.portability_cancel))
                    }
                }
            }

            if (active) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(progressColor()),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (percent == null) 0.33f else percent / 100f)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }

            job.result.forEach { (category, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(categoryLabel(category), style = MaterialTheme.typography.labelMedium)
                    Text(count.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${stringResource(R.string.portability_job_label)}: ${job.id}", style = MaterialTheme.typography.labelSmall)
                    job.requestId?.let {
                        Text("${stringResource(R.string.portability_request_label)}: $it", style = MaterialTheme.typography.labelSmall)
                    }
                    job.errorCode?.let {
                        Text("${stringResource(R.string.portability_code_label)}: $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = {
                    clipboard.setText(
                        AnnotatedString(listOfNotNull(job.id, job.requestId, job.errorCode).joinToString("\n")),
                    )
                    copied = true
                }) {
                    Icon(if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy, null)
                }
            }
        }
    }
}

@Composable
private fun progressColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

@Composable
private fun PortabilityJob.stateLabel(): String = when (state) {
    PortabilityJobState.Queued -> stringResource(R.string.portability_job_waiting)
    PortabilityJobState.Analyzing -> stringResource(R.string.portability_job_analyzing)
    PortabilityJobState.Ready -> stringResource(R.string.portability_job_ready)
    PortabilityJobState.Applying -> stringResource(R.string.portability_job_importing)
    PortabilityJobState.Encoding -> stringResource(R.string.portability_job_generating)
    PortabilityJobState.Completed -> stringResource(
        if (kind == dev.typetype.android.domain.imports.PortabilityDirection.Import) {
            R.string.portability_job_import_completed
        } else {
            R.string.portability_job_export_ready
        },
    )
    PortabilityJobState.Cancelled -> stringResource(R.string.portability_job_cancelled)
    PortabilityJobState.Failed -> stringResource(R.string.portability_job_failed)
}
