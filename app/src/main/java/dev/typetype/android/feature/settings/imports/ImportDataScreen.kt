package dev.typetype.android.feature.settings.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportDataRoute(
    onNavigateBack: () -> Unit,
    viewModel: ImportDataViewModel = hiltViewModel(),
    youtubeViewModel: YoutubeTakeoutImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val youtubeState by youtubeViewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val exportName = remember {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        "typetype-backup-$date.json"
    }
    val exporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let(viewModel::exportTypeType) },
    )
    val typeTypePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::selectTypeTypeDocument) },
    )
    val pipePipePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::selectPipePipeDocument) },
    )
    val youtubeTakeoutPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = youtubeViewModel::selectDocuments,
    )
    ImportDataScreen(
        state = state,
        youtubeState = youtubeState,
        onNavigateBack = onNavigateBack,
        onToggleCategory = viewModel::toggleTypeTypeCategory,
        onExportTypeType = { exporter.launch(exportName) },
        onChooseTypeTypeBackup = {
            typeTypePicker.launch(
                arrayOf(
                    "application/json",
                    "text/json",
                    "text/plain",
                    "application/octet-stream",
                ),
            )
        },
        onRestoreTypeType = viewModel::restoreTypeType,
        onDismissTypeTypeRestore = viewModel::dismissTypeTypeRestore,
        onResetTypeTypeResult = viewModel::resetTypeTypeResult,
        onChoosePipePipeBackup = {
            pipePipePicker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        },
        onRestorePipePipe = viewModel::restorePipePipe,
        onResetPipePipeResult = viewModel::resetPipePipeResult,
        onOpenYoutubeTakeout = { uriHandler.openUri(YOUTUBE_TAKEOUT_URL) },
        onChooseYoutubeTakeout = {
            youtubeTakeoutPicker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        },
        onRetryYoutubeTakeout = youtubeViewModel::retry,
        onCancelYoutubeTakeout = youtubeViewModel::cancel,
        onRemoveYoutubeTakeout = youtubeViewModel::remove,
        onRetryYoutubeRefresh = youtubeViewModel::retryCollectionRefresh,
    )
}

@Composable
fun ImportDataScreen(
    state: ImportDataState,
    youtubeState: YoutubeTakeoutImportState,
    onNavigateBack: () -> Unit,
    onToggleCategory: (TypeTypeBackupCategory) -> Unit,
    onExportTypeType: () -> Unit,
    onChooseTypeTypeBackup: () -> Unit,
    onRestoreTypeType: () -> Unit,
    onDismissTypeTypeRestore: () -> Unit,
    onResetTypeTypeResult: () -> Unit,
    onChoosePipePipeBackup: () -> Unit,
    onRestorePipePipe: () -> Unit,
    onResetPipePipeResult: () -> Unit,
    onOpenYoutubeTakeout: () -> Unit,
    onChooseYoutubeTakeout: () -> Unit,
    onRetryYoutubeTakeout: (String) -> Unit,
    onCancelYoutubeTakeout: (String) -> Unit,
    onRemoveYoutubeTakeout: (String) -> Unit,
    onRetryYoutubeRefresh: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ImportTopBar(onNavigateBack)
            TypeTypeBackupSection(
                state = state,
                onToggleCategory = onToggleCategory,
                onExport = onExportTypeType,
                onChooseBackup = onChooseTypeTypeBackup,
                onResetResult = onResetTypeTypeResult,
            )
            YoutubeTakeoutImportSection(
                state = youtubeState,
                onOpenTakeout = onOpenYoutubeTakeout,
                onChooseArchives = onChooseYoutubeTakeout,
                onRetry = onRetryYoutubeTakeout,
                onCancel = onCancelYoutubeTakeout,
                onRemove = onRemoveYoutubeTakeout,
                onRetryCollectionRefresh = onRetryYoutubeRefresh,
            )
            PipePipeImportSection(
                state = state,
                onChooseBackup = onChoosePipePipeBackup,
                onRestore = onRestorePipePipe,
                onResetResult = onResetPipePipeResult,
            )
            importErrorMessage(state.errorKey)?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.errorRequestId?.let { RequestIdRow(requestId = it) }
        }
    }
    state.selectedTypeTypeDocument?.let { document ->
        TypeTypeRestoreDialog(
            documentName = document.displayName,
            restoring = state.isRestoringTypeType,
            onConfirm = onRestoreTypeType,
            onDismiss = onDismissTypeTypeRestore,
        )
    }
}

private const val YOUTUBE_TAKEOUT_URL =
    "https://takeout.google.com/settings/takeout/custom/youtube,my_activity?dest=mail&frequency=once"

@Composable
private fun ImportTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
            )
        }
        Text(
            text = stringResource(R.string.settings_import_title),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun importErrorMessage(key: String?): String? = when (key) {
    null -> null
    "BACKUP_NO_CATEGORIES" -> stringResource(R.string.settings_backup_select_category)
    "BACKUP_FILE_NOT_JSON" -> stringResource(R.string.settings_backup_invalid_json)
    "BACKUP_DESTINATION_UNAVAILABLE" -> stringResource(R.string.settings_backup_unavailable)
    "IMPORT_FILE_NOT_ZIP" -> stringResource(R.string.settings_import_invalid_zip)
    "IMPORT_FILE_TOO_LARGE" -> stringResource(R.string.settings_import_too_large)
    "IMPORT_FILE_EMPTY" -> stringResource(R.string.settings_import_empty)
    "IMPORT_FILE_UNAVAILABLE" -> stringResource(R.string.settings_import_unavailable)
    else -> stringResource(R.string.settings_import_failed)
}
