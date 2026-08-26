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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy

@Composable
internal fun PortabilityPanel(
    state: PortabilityUiState,
    onModeSelected: (PortabilityScreenMode) -> Unit,
    onFormatSelected: (dev.typetype.android.domain.imports.PortabilityFormat) -> Unit,
    onCategoryToggled: (dev.typetype.android.domain.imports.TypeTypeBackupCategory) -> Unit,
    onPolicySelected: (PortabilityDuplicatePolicy) -> Unit,
    onStartExport: () -> Unit,
    onChooseFile: () -> Unit,
    onApplyImport: () -> Unit,
    onCancelJob: () -> Unit,
    onResetJob: () -> Unit,
    onDownloadArtifact: () -> Unit,
    onDownloadReport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            PortabilityScreenMode.entries.forEach { mode ->
                val active = state.mode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable(role = Role.Tab) { onModeSelected(mode) },
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                    contentColor = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (mode == PortabilityScreenMode.Import) {
                                Icons.Filled.UploadFile
                            } else {
                                Icons.Filled.Download
                            },
                            contentDescription = null,
                            modifier = Modifier.height(15.dp),
                        )
                        Text(
                            text = stringResource(
                                if (mode == PortabilityScreenMode.Import) {
                                    R.string.settings_portability_import
                                } else {
                                    R.string.settings_portability_export
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        if (state.isLoadingFormats) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
                Text(stringResource(R.string.portability_loading_formats), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            when (state.mode) {
                PortabilityScreenMode.Export -> ExportPanel(
                    state = state,
                    onFormatSelected = onFormatSelected,
                    onCategoryToggled = onCategoryToggled,
                    onStartExport = onStartExport,
                    onCancelJob = onCancelJob,
                    onResetJob = onResetJob,
                    onDownloadArtifact = onDownloadArtifact,
                    onDownloadReport = onDownloadReport,
                )
                PortabilityScreenMode.Import -> ImportPanel(
                    state = state,
                    onFormatSelected = onFormatSelected,
                    onCategoryToggled = onCategoryToggled,
                    onPolicySelected = onPolicySelected,
                    onChooseFile = onChooseFile,
                    onApplyImport = onApplyImport,
                    onCancelJob = onCancelJob,
                    onResetJob = onResetJob,
                    onDownloadReport = onDownloadReport,
                )
            }
        }
        state.failureMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.failureCode?.let { code ->
            Text(
                text = stringResource(R.string.error_code, code),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        state.failureRequestId?.let { requestId ->
            RequestIdRow(requestId = requestId)
        }
    }
}

@Composable
internal fun SectionHeader(icon: @Composable () -> Unit, title: Int, subtitle: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        icon()
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
