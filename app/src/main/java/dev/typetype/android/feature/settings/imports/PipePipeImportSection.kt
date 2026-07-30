package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.domain.imports.PipePipeRestoreSummary

@Composable
internal fun PipePipeImportSection(
    state: ImportDataState,
    onChooseBackup: () -> Unit,
    onRestore: () -> Unit,
    onResetResult: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_import_pipepipe_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
        text = stringResource(R.string.settings_import_pipepipe_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.pipePipeSummary?.let { summary ->
        PipePipeRestoreSummaryCard(summary, onResetResult)
    } ?: TypeTypeCard {
        TypeTypeSecondaryButton(
            text = state.selectedPipePipeDocument?.displayName
                ?: stringResource(R.string.settings_import_choose_file),
            onClick = onChooseBackup,
            enabled = !state.isRestoringPipePipe,
        )
        TypeTypePrimaryButton(
            text = stringResource(R.string.settings_import_restore),
            onClick = onRestore,
            enabled = state.canRestorePipePipe,
            isLoading = state.isRestoringPipePipe,
        )
    }
}

@Composable
private fun PipePipeRestoreSummaryCard(
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
            modifier = Modifier.padding(top = 8.dp),
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
