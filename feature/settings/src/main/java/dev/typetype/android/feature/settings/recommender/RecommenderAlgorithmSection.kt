package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.AlgorithmInfo

internal fun LazyListScope.algorithmSection(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    val current = state.selectedAlgorithm ?: return
    item(key = "choice") {
        RecommenderGroup(
            label = stringResource(R.string.algo_choice_label),
            icon = Icons.AutoMirrored.Outlined.ViewList,
            help = stringResource(R.string.algo_help_choice),
        ) {
            state.algorithms.forEach { algorithm ->
                AlgorithmChoiceRow(algorithm, algorithm.id == current.id) {
                    onAction(RecommenderSettingsAction.ChooseAlgorithm(algorithm.id))
                }
            }
            RecommenderRow(
                label = stringResource(R.string.algo_guide_link),
                description = stringResource(R.string.algo_guide_link_description),
                icon = Icons.Outlined.Explore,
                onClick = { onAction(RecommenderSettingsAction.Open(RecommenderSection.Guide)) },
                divider = false,
            ) { Chevron() }
        }
    }
    item(key = "featured-${current.id}") {
        RecommenderGroup(label = stringResource(R.string.algo_settings_of, current.name), icon = Icons.Outlined.Tune) {
            current.parameters.filter { it.featured }.forEach { parameter ->
                ParameterRow(parameter, state.valueOf(parameter)) { value ->
                    onAction(RecommenderSettingsAction.SetParameter(parameter.id, value))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            RecommenderRow(
                label = stringResource(R.string.algo_all_settings),
                description = pluralStringResource(R.plurals.algo_all_settings_description, current.parameters.size, current.parameters.size),
                onClick = { onAction(RecommenderSettingsAction.Open(RecommenderSection.Details)) },
                divider = false,
            ) { Chevron() }
        }
    }
    item(key = "transparency") {
        val uriHandler = LocalUriHandler.current
        val uses = current.uses.map { dataLabel(it) }.joinToString(", ")
        RecommenderGroup(label = stringResource(R.string.algo_transparency_label), icon = Icons.Outlined.Code) {
            RecommenderRow(
                label = stringResource(R.string.algo_uses_label),
                description = uses.ifEmpty { stringResource(R.string.algo_uses_nothing) },
                icon = Icons.Outlined.Storage,
                divider = current.sourceUrl != null,
            )
            current.sourceUrl?.let { url ->
                RecommenderRow(
                    label = stringResource(R.string.algo_code_label),
                    description = stringResource(R.string.algo_code_description),
                    icon = Icons.Outlined.Code,
                    onClick = { uriHandler.openUri(url) },
                    divider = false,
                ) { Chevron() }
            }
        }
    }
}

@Composable
private fun AlgorithmChoiceRow(algorithm: AlgorithmInfo, selected: Boolean, onSelect: () -> Unit) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "algorithm-icon",
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(algorithmIcon(algorithm), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(algorithm.name, style = MaterialTheme.typography.bodyLarge)
                Text(algorithm.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

internal fun algorithmIcon(algorithm: AlgorithmInfo): ImageVector = when {
    "history" in algorithm.uses -> Icons.Outlined.Radar
    "trending" in algorithm.sources -> Icons.Outlined.Explore
    "subscriptions" in algorithm.uses -> Icons.Outlined.NotificationsNone
    else -> Icons.AutoMirrored.Outlined.ViewList
}
