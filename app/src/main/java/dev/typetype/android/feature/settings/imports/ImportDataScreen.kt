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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.domain.imports.PipePipeRestoreSummary

@Composable
fun ImportDataRoute(
    onNavigateBack: () -> Unit,
    viewModel: ImportDataViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::selectDocument) }
    ImportDataScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onChooseFile = {
            picker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        },
        onRestore = viewModel::restore,
        onReset = viewModel::reset,
    )
}

@Composable
fun ImportDataScreen(
    state: ImportDataState,
    onNavigateBack: () -> Unit,
    onChooseFile: () -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit,
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
            Text(
                text = stringResource(R.string.settings_import_pipepipe_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = stringResource(R.string.settings_import_pipepipe_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.summary?.let {
                RestoreSummaryCard(summary = it, onReset = onReset)
            } ?: TypeTypeCard {
                TypeTypeSecondaryButton(
                    text = state.selectedDocument?.displayName
                        ?: stringResource(R.string.settings_import_choose_file),
                    onClick = onChooseFile,
                    enabled = !state.isRestoring,
                )
                TypeTypePrimaryButton(
                    text = stringResource(R.string.settings_import_restore),
                    onClick = onRestore,
                    enabled = state.canRestore,
                    isLoading = state.isRestoring,
                )
            }
            importErrorMessage(state.errorKey)?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.errorRequestId?.let { RequestIdRow(requestId = it) }
        }
    }
}

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
private fun RestoreSummaryCard(
    summary: PipePipeRestoreSummary,
    onReset: () -> Unit,
) {
    TypeTypeCard {
        Text(
            text = stringResource(R.string.settings_import_complete),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SummaryRow(R.string.settings_import_history, summary.history)
        SummaryRow(R.string.settings_import_subscriptions, summary.subscriptions)
        SummaryRow(R.string.settings_import_playlists, summary.playlists)
        SummaryRow(R.string.settings_import_playlist_videos, summary.playlistVideos)
        SummaryRow(R.string.settings_import_progress, summary.progress)
        SummaryRow(R.string.settings_import_search_history, summary.searchHistory)
        TypeTypeSecondaryButton(
            text = stringResource(R.string.settings_import_another),
            onClick = onReset,
        )
    }
}

@Composable
private fun SummaryRow(labelRes: Int, count: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = count.toString(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun importErrorMessage(key: String?): String? = when (key) {
    null -> null
    "IMPORT_FILE_NOT_ZIP" -> stringResource(R.string.settings_import_invalid_zip)
    "IMPORT_FILE_TOO_LARGE" -> stringResource(R.string.settings_import_too_large)
    "IMPORT_FILE_EMPTY" -> stringResource(R.string.settings_import_empty)
    "IMPORT_FILE_UNAVAILABLE" -> stringResource(R.string.settings_import_unavailable)
    else -> stringResource(R.string.settings_import_failed)
}
