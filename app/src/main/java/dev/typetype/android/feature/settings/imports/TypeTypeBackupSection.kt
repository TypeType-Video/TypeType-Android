package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import dev.typetype.android.domain.imports.TypeTypeRestoreSummary

@Composable
internal fun TypeTypeBackupSection(
    state: ImportDataState,
    onToggleCategory: (TypeTypeBackupCategory) -> Unit,
    onExport: () -> Unit,
    onChooseBackup: () -> Unit,
    onResetResult: () -> Unit,
    showExportControls: Boolean = false,
    showRestoreControls: Boolean = false,
) {
    Text(
        text = stringResource(R.string.settings_backup_typetype_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
    if (showExportControls) {
        Text(
            text = stringResource(R.string.settings_backup_typetype_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.typeTypeSummary?.let { summary ->
        TypeTypeRestoreSummaryCard(summary, onResetResult)
    } ?: TypeTypeCard {
        if (showExportControls) {
            TypeTypeBackupCategory.entries.forEach { category ->
                BackupCategoryRow(
                    category = category,
                    checked = category in state.selectedCategories,
                    enabled = !state.isExportingTypeType && !state.isRestoringTypeType,
                    onToggle = { onToggleCategory(category) },
                )
            }
            TypeTypePrimaryButton(
                text = stringResource(R.string.settings_backup_export_selected),
                onClick = onExport,
                enabled = state.canExportTypeType,
                isLoading = state.isExportingTypeType,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (state.typeTypeExportComplete) {
                Text(
                    text = stringResource(R.string.settings_backup_export_complete),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (showRestoreControls) {
            TypeTypeSecondaryButton(
                text = stringResource(R.string.settings_backup_restore_typetype),
                onClick = onChooseBackup,
                enabled = !state.isExportingTypeType && !state.isRestoringTypeType,
            )
        }
        Text(
            text = stringResource(R.string.settings_backup_replace_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun BackupCategoryRow(
    category: TypeTypeBackupCategory,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        Text(text = stringResource(category.labelRes()), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TypeTypeRestoreSummaryCard(
    summary: TypeTypeRestoreSummary,
    onReset: () -> Unit,
) {
    TypeTypeCard {
        Text(
            text = pluralStringResource(
                R.plurals.settings_backup_restore_complete,
                summary.total,
                summary.total,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        summary.restored.forEach { (category, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(category.labelRes()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = count.toString(), fontWeight = FontWeight.SemiBold)
            }
        }
        TypeTypeSecondaryButton(
            text = stringResource(R.string.settings_backup_done),
            onClick = onReset,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun TypeTypeRestoreDialog(
    documentName: String,
    restoring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_backup_confirm_title)) },
        text = {
            Text(stringResource(R.string.settings_backup_confirm_message, documentName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !restoring) {
                Text(stringResource(R.string.settings_import_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !restoring) {
                Text(stringResource(R.string.settings_backup_cancel))
            }
        },
    )
}

private fun TypeTypeBackupCategory.labelRes(): Int = when (this) {
    TypeTypeBackupCategory.Subscriptions -> R.string.settings_import_subscriptions
    TypeTypeBackupCategory.SubscriptionGroups -> R.string.settings_import_subscription_groups
    TypeTypeBackupCategory.History -> R.string.settings_import_history
    TypeTypeBackupCategory.Playlists -> R.string.settings_import_playlists
    TypeTypeBackupCategory.WatchLater -> R.string.settings_backup_watch_later
    TypeTypeBackupCategory.Favorites -> R.string.settings_backup_favorites
    TypeTypeBackupCategory.Progress -> R.string.settings_import_progress
    TypeTypeBackupCategory.SearchHistory -> R.string.settings_import_search_history
    TypeTypeBackupCategory.SavedPlaylists -> R.string.settings_backup_saved_playlists
    TypeTypeBackupCategory.Settings -> R.string.settings_backup_settings
    TypeTypeBackupCategory.ContentFilters -> R.string.settings_backup_content_filters
}
