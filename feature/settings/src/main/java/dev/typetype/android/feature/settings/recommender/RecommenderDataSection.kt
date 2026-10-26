package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.domain.recommendations.DataCategory

internal fun LazyListScope.dataSection(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    val categories = when (val data = state.data) {
        Loadable.Failed -> {
            item { LoadFailed { onAction(RecommenderSettingsAction.Retry) } }
            return
        }
        is Loadable.Loaded -> data.value
        else -> {
            item { RecommenderNote(stringResource(R.string.algo_loading)) }
            return
        }
    }
    val open = categories.firstOrNull { it.id == state.openCategory }
    if (open != null) {
        dataDetail(open, state.collectHistory, onAction)
        return
    }
    item(key = "data") {
        RecommenderGroup(
            label = stringResource(R.string.algo_data_label),
            icon = Icons.Outlined.Storage,
            help = stringResource(R.string.algo_help_stored_data),
        ) {
            categories.forEachIndexed { index, category ->
                CategoryRow(category, state, divider = index < categories.lastIndex, onAction = onAction)
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: DataCategory,
    state: RecommenderSettingsState,
    divider: Boolean,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    val current = state.selectedAlgorithm
    val name = current?.name.orEmpty()
    val read = current?.uses?.contains(category.id) == true
    val status = when {
        category.collect == false -> stringResource(R.string.algo_not_collected)
        category.used && read -> stringResource(R.string.algo_used_by, name)
        category.used -> stringResource(R.string.algo_not_needed_by, name)
        else -> stringResource(R.string.algo_kept_not_used)
    }
    val label = dataLabel(category.id)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .clickable(role = Role.Button) { onAction(RecommenderSettingsAction.OpenCategory(category.id)) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(dataIcon(category.id), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${itemCount(category.count)} · $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (category.used && read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            UseSwitch(label, category.used) { onAction(RecommenderSettingsAction.ToggleUse(category.id)) }
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
internal fun UseSwitch(label: String, checked: Boolean, onToggle: () -> Unit) {
    val description = stringResource(R.string.algo_use_category, label)
    TypeTypeSwitch(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = Modifier.padding(start = 8.dp).semantics { contentDescription = description },
    )
}
