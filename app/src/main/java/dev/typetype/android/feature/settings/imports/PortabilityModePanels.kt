package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityFidelity
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.PortabilityJobState
import dev.typetype.android.domain.imports.TypeTypeBackupCategory

@Composable
internal fun ExportPanel(
    state: PortabilityUiState,
    onFormatSelected: (PortabilityFormat) -> Unit,
    onSelectAllCategories: (Set<TypeTypeBackupCategory>) -> Unit,
    onCategoryToggled: (TypeTypeBackupCategory) -> Unit,
    onStartExport: () -> Unit,
    onCancelJob: () -> Unit,
    onResetJob: () -> Unit,
    onDownloadArtifact: () -> Unit,
    onDownloadReport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionHeader(icon = {
            Icon(Icons.Filled.Download, contentDescription = null)
        }, title = R.string.portability_export_title, subtitle = R.string.portability_export_description)

        val job = state.job
        if (job == null) {
            if (state.formats.isEmpty()) {
                Text(
                    text = stringResource(R.string.portability_no_export_format),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PortabilityFormatPicker(
                labelRes = R.string.portability_destination_format,
                formats = state.formats,
                selected = state.selectedFormat,
                onSelect = onFormatSelected,
            )
            if (state.selectedFormat != null) {
                val available = TypeTypeBackupCategory.entries.filter { category ->
                    state.selectedFormat.supports(state.mode.direction, category)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = available.isNotEmpty() &&
                            available.all(state.selectedCategories::contains),
                        onCheckedChange = { checked ->
                            onSelectAllCategories(
                                if (checked) available.toSet() else emptySet(),
                            )
                        },
                    )
                    Text(stringResource(R.string.portability_select_all))
                }
                PortabilityCategoryGrid(
                    format = state.selectedFormat,
                    direction = state.mode.direction,
                    selected = state.selectedCategories,
                    showStatuses = true,
                    onToggle = onCategoryToggled,
                )
                if (state.selectedFormat.capabilities.any { capability ->
                    PortabilityDirection.Export in capability.directions &&
                        capability.fidelity == PortabilityFidelity.Partial
                }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text(
                            stringResource(R.string.portability_export_partial_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            if (state.selectedFormat != null) {
                HorizontalDivider()
                PanelButton(
                    text = stringResource(R.string.portability_generate_export),
                    enabled = state.selectedCategories.isNotEmpty() && !state.isStartingJob,
                    emphasized = true,
                    onClick = onStartExport,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        } else {
            PortabilityJobCard(job, state.isCancelling, onCancelJob)
            if (job.state == PortabilityJobState.Completed) {
                job.preview?.issues?.takeIf { issues -> issues.isNotEmpty() }?.let { issues ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(stringResource(R.string.portability_export_notes), style = MaterialTheme.typography.labelMedium)
                            issues.forEach { issue ->
                                Text("${issue.message} (${issue.count})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelButton(
                        text = stringResource(R.string.portability_download_export),
                        enabled = !state.isSavingArtifact,
                        emphasized = true,
                        onClick = onDownloadArtifact,
                    )
                    PanelButton(stringResource(R.string.portability_new_export), onClick = onResetJob)
                    PanelButton(
                        stringResource(R.string.portability_download_report),
                        enabled = true,
                        onClick = onDownloadReport,
                    )
                }
            }
            if (job.state.isTerminalUiState()) {
                PanelButton(stringResource(R.string.portability_start_another_export), onClick = onResetJob)
            }
        }
    }
}

@Composable
internal fun ImportPanel(
    state: PortabilityUiState,
    onFormatSelected: (PortabilityFormat) -> Unit,
    onCategoryToggled: (TypeTypeBackupCategory) -> Unit,
    onPolicySelected: (PortabilityDuplicatePolicy) -> Unit,
    onChooseFile: () -> Unit,
    onApplyImport: () -> Unit,
    onCancelJob: () -> Unit,
    onResetJob: () -> Unit,
    onDownloadReport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionHeader(icon = {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
        }, title = R.string.portability_import_title, subtitle = R.string.portability_import_description)
        val job = state.job
        when {
            job == null -> {
                if (state.formats.isEmpty()) {
                    Text(
                        text = stringResource(R.string.portability_no_import_format),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                EmptyImportForm(state, onFormatSelected, onChooseFile)
            }
            job.state == PortabilityJobState.Ready -> ReadyImportPreview(
                state = state,
                onCategoryToggled = onCategoryToggled,
                onPolicySelected = onPolicySelected,
                onApplyImport = onApplyImport,
                onResetJob = onResetJob,
            )
            else -> {
                PortabilityJobCard(job, state.isCancelling, onCancelJob)
                if (job.state.isTerminalUiState()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelButton(
                            stringResource(R.string.portability_start_another_import),
                            onClick = onResetJob,
                        )
                        PanelButton(
                            stringResource(R.string.portability_download_report),
                            enabled = true,
                            onClick = onDownloadReport,
                        )
                    }
                }
            }
        }
    }
}
