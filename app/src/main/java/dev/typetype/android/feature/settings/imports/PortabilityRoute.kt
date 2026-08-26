package dev.typetype.android.feature.settings.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun PortabilityRoute(
    onNavigateBack: () -> Unit,
    viewModel: PortabilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeJob = state.job
    BackHandler(enabled = activeJob != null && !activeJob.isTerminal) { }
    val artifactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri -> uri?.let(viewModel::downloadArtifact) },
    )
    val reportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let(viewModel::downloadReport) },
    )
    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::chooseImportFile) },
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_import_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_import_portability_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_import_portability_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    PortabilityPanel(
                        state = state,
                        onModeSelected = viewModel::selectMode,
                        onFormatSelected = viewModel::selectFormat,
                        onSelectAllCategories = viewModel::setExportSelection,
                        onCategoryToggled = viewModel::toggleCategory,
                        onPolicySelected = viewModel::setDuplicatePolicy,
                        onStartExport = viewModel::startExport,
                        onChooseFile = {
                            importPicker.launch(arrayOf("*/*"))
                        },
                        onApplyImport = viewModel::applyImport,
                        onCancelJob = viewModel::cancel,
                        onResetJob = viewModel::resetJob,
                        onDownloadArtifact = {
                            val format = state.selectedFormat
                            val id = state.job?.id
                            if (format != null && id != null) {
                                artifactPicker.launch("typetype-$id.${format.defaultExtension}")
                            } else {
                                artifactPicker.launch("typetype-export")
                            }
                        },
                        onDownloadReport = {
                            val id = state.job?.id
                            reportPicker.launch("typetype-report-$id.json")
                        },
                    )
                }
                if (state.isLoadingFormats) {
                    item { CircularProgressIndicator() }
                }
            }
        }
    }
}
