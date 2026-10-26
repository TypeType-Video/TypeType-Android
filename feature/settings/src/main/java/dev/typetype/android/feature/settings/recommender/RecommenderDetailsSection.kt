package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

internal fun LazyListScope.detailsSection(
    state: RecommenderSettingsState,
    open: Set<String>,
    onToggle: (String) -> Unit,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    val current = state.selectedAlgorithm ?: return
    item(key = "details-${current.id}") {
        RecommenderGroup(label = stringResource(R.string.algo_all_settings_of, current.name), icon = Icons.Outlined.Tune) {
            val groups = current.parameters.groupBy { it.group }.entries.toList()
            groups.forEachIndexed { index, (group, parameters) ->
                val changed = parameters.count { !it.isDefault(state.valueOf(it)) }
                val text = groupText(group)
                RecommenderFold(
                    icon = text.icon,
                    label = stringResource(text.title),
                    description = stringResource(text.help),
                    summary = if (changed > 0) {
                        pluralStringResource(R.plurals.algo_changed_count, changed, changed)
                    } else {
                        pluralStringResource(R.plurals.algo_settings_count, parameters.size, parameters.size)
                    },
                    highlightSummary = changed > 0,
                    open = group in open,
                    onToggle = { onToggle(group) },
                    divider = index < groups.lastIndex,
                ) {
                    parameters.forEach { parameter ->
                        ParameterRow(
                            parameter = parameter,
                            value = state.valueOf(parameter),
                            showDefault = true,
                            onRestore = { onAction(RecommenderSettingsAction.RestoreParameter(parameter.id)) },
                        ) { value -> onAction(RecommenderSettingsAction.SetParameter(parameter.id, value)) }
                    }
                }
            }
        }
    }
    item(key = "last-home") {
        RecommenderGroup(
            label = stringResource(R.string.algo_last_home_label),
            icon = Icons.Outlined.Home,
            help = stringResource(R.string.algo_help_last_home),
        ) {
            LastHome(state, open = LAST_HOME in open, onToggle = { onToggle(LAST_HOME) }, onRetry = { onAction(RecommenderSettingsAction.Retry) })
        }
    }
    item(key = "reset") {
        var confirming by rememberSaveable { mutableStateOf(false) }
        RecommenderGroup(label = stringResource(R.string.algo_reset_all, current.name), icon = Icons.Outlined.RestartAlt) {
            RecommenderRow(label = stringResource(R.string.algo_reset_description), divider = false) {
                TextAction(stringResource(R.string.algo_restore), { confirming = true })
            }
        }
        if (confirming) {
            AlertDialog(
                onDismissRequest = { confirming = false },
                title = { Text(stringResource(R.string.algo_reset_question, current.name)) },
                text = { Text(stringResource(R.string.algo_reset_description)) },
                confirmButton = {
                    TextButton(onClick = {
                        onAction(RecommenderSettingsAction.ResetParameters)
                        confirming = false
                    }) { Text(stringResource(R.string.algo_restore)) }
                },
                dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.algo_cancel)) } },
            )
        }
    }
}

@Composable
internal fun RecommenderFold(
    icon: ImageVector,
    label: String,
    description: String,
    summary: String,
    open: Boolean,
    onToggle: () -> Unit,
    highlightSummary: Boolean = false,
    divider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "fold")
    val expandedText = stringResource(if (open) R.string.algo_expanded else R.string.algo_collapsed)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = expandedText }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (highlightSummary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(visible = open) {
            Column(modifier = Modifier.padding(start = 32.dp, bottom = 8.dp), content = content)
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

private const val LAST_HOME = "last-home"
