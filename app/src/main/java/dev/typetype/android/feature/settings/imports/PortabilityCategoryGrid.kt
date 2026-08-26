package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
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
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityFidelity
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.TypeTypeBackupCategory

@Composable
internal fun PortabilityCategoryGrid(
    format: PortabilityFormat?,
    direction: PortabilityDirection,
    selected: Set<TypeTypeBackupCategory>,
    counts: Map<TypeTypeBackupCategory, Int> = emptyMap(),
    showStatuses: Boolean = false,
    onToggle: (TypeTypeBackupCategory) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            TypeTypeBackupCategory.entries.forEachIndexed { index, category ->
                val supported = format?.supports(direction, category) == true
                val enabled = supported && (counts.isEmpty() || counts.containsKey(category))
                if (index != 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CategoryRow(
                    category = category,
                    selected = selected,
                    counts = counts,
                    showStatuses = showStatuses,
                    format = format,
                    direction = direction,
                    supported = supported,
                    enabled = enabled,
                    onToggle = onToggle,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: TypeTypeBackupCategory,
    selected: Set<TypeTypeBackupCategory>,
    counts: Map<TypeTypeBackupCategory, Int>,
    showStatuses: Boolean,
    format: PortabilityFormat?,
    direction: PortabilityDirection,
    supported: Boolean,
    enabled: Boolean,
    onToggle: (TypeTypeBackupCategory) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .clickable(enabled = enabled, role = Role.Checkbox) { onToggle(category) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = category in selected, onCheckedChange = { onToggle(category) }, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryLabel(category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (supported) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                counts[category]?.let { count ->
                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                }
                if (showStatuses) CategoryStatusIcon(format, direction, category)
            }
            Text(
                text = categoryDetail(category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryStatusIcon(
    format: PortabilityFormat?,
    direction: PortabilityDirection,
    category: TypeTypeBackupCategory,
) {
    val capability = format?.capabilities?.firstOrNull {
        it.category == category && direction in it.directions
    } ?: return
    if (capability.fidelity == PortabilityFidelity.Partial) {
        Icon(Icons.Outlined.Warning, contentDescription = null)
    } else {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
    }
}

@Composable
internal fun categoryLabel(category: TypeTypeBackupCategory): String = when (category) {
    TypeTypeBackupCategory.Subscriptions -> stringResource(R.string.settings_import_subscriptions)
    TypeTypeBackupCategory.SubscriptionGroups -> stringResource(R.string.settings_import_subscription_groups)
    TypeTypeBackupCategory.History -> stringResource(R.string.settings_import_history)
    TypeTypeBackupCategory.Playlists -> stringResource(R.string.settings_import_playlists)
    TypeTypeBackupCategory.WatchLater -> stringResource(R.string.settings_backup_watch_later)
    TypeTypeBackupCategory.Favorites -> stringResource(R.string.settings_backup_favorites)
    TypeTypeBackupCategory.Progress -> stringResource(R.string.settings_import_progress)
    TypeTypeBackupCategory.SearchHistory -> stringResource(R.string.settings_import_search_history)
    TypeTypeBackupCategory.SavedPlaylists -> stringResource(R.string.settings_backup_saved_playlists)
    TypeTypeBackupCategory.Settings -> stringResource(R.string.settings_backup_settings)
    TypeTypeBackupCategory.ContentFilters -> stringResource(R.string.settings_backup_content_filters)
}

@Composable
private fun categoryDetail(category: TypeTypeBackupCategory): String = when (category) {
    TypeTypeBackupCategory.Subscriptions ->
        stringResource(R.string.portability_category_subscriptions_detail)
    TypeTypeBackupCategory.SubscriptionGroups ->
        stringResource(R.string.portability_category_subscription_groups_detail)
    TypeTypeBackupCategory.History -> stringResource(R.string.portability_category_history_detail)
    TypeTypeBackupCategory.Playlists -> stringResource(R.string.portability_category_playlists_detail)
    TypeTypeBackupCategory.WatchLater -> stringResource(R.string.portability_category_watch_later_detail)
    TypeTypeBackupCategory.Favorites -> stringResource(R.string.portability_category_favorites_detail)
    TypeTypeBackupCategory.Progress -> stringResource(R.string.portability_category_progress_detail)
    TypeTypeBackupCategory.SearchHistory ->
        stringResource(R.string.portability_category_search_history_detail)
    TypeTypeBackupCategory.SavedPlaylists ->
        stringResource(R.string.portability_category_saved_playlists_detail)
    TypeTypeBackupCategory.Settings -> stringResource(R.string.portability_category_settings_detail)
    TypeTypeBackupCategory.ContentFilters ->
        stringResource(R.string.portability_category_content_filters_detail)
}
