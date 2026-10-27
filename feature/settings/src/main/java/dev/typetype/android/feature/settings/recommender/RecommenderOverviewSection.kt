package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R

internal fun LazyListScope.overviewSection(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    if (!state.preferences.enabled) {
        item { RecommenderNote(stringResource(R.string.algo_overview_off)) }
        return
    }
    when (val home = state.home) {
        Loadable.Failed -> item { LoadFailed { onAction(RecommenderSettingsAction.Retry) } }
        is Loadable.Loaded -> if (home.value.algorithm == null) {
            item { RecommenderNote(stringResource(R.string.algo_overview_fallback)) }
            return
        }
        else -> {
            item { RecommenderNote(stringResource(R.string.algo_loading)) }
            return
        }
    }
    val rows = state.home.value?.rows.orEmpty()
    item(key = "composition") {
        val labels = rows.map { reasonLabel(it.reason) to it.videoUrls.size }
        val parts = labels.groupBy({ it.first }, { it.second }).map { (label, counts) -> DonutPart(label, label, counts.sum().toDouble()) }
        RecommenderGroup(
            label = stringResource(R.string.algo_composition_label),
            icon = Icons.Outlined.PieChart,
            help = stringResource(R.string.algo_help_composition),
        ) {
            state.selectedAlgorithm?.let {
                Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Donut(
                parts = parts,
                centerValue = rows.sumOf { it.videoUrls.size },
                centerLabel = stringResource(R.string.algo_composition_center),
                unit = { count -> pluralStringResource(R.plurals.algo_videos, count, count) },
            )
            TextAction(stringResource(R.string.algo_adjust), { onAction(RecommenderSettingsAction.Open(RecommenderSection.Algorithm)) })
        }
    }
    item(key = "share") { InterestShare(state, onAction) }
    item(key = "rows") {
        RecommenderGroup(
            label = stringResource(R.string.algo_rows_label),
            icon = Icons.AutoMirrored.Outlined.ViewList,
            help = stringResource(R.string.algo_rows_description),
        ) {
            rows.forEachIndexed { index, row ->
                RecommenderRow(label = row.title, description = reasonLabel(row.reason), divider = index < rows.lastIndex) {
                    Text(
                        pluralStringResource(R.plurals.algo_videos, row.videoUrls.size, row.videoUrls.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun InterestShare(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    val profile = state.profile.value ?: return
    val ignored = state.preferences.ignoredSignals.toSet()
    val counted = profile.groups.filter { !it.muted && it.id !in state.preferences.mutedGroups && it.id !in ignored }
    val heaviest = counted.sortedByDescending { it.weight }
    val parts = heaviest.take(SHARE_PARTS).map { DonutPart(it.id, it.label, it.weight) }.toMutableList()
    val others = heaviest.drop(SHARE_PARTS).sumOf { it.weight }
    val othersLabel = pluralStringResource(R.plurals.algo_share_others, heaviest.size - SHARE_PARTS, heaviest.size - SHARE_PARTS)
    if (others > 0) parts += DonutPart(OTHERS, othersLabel, others, neutral = true)
    RecommenderGroup(
        label = stringResource(R.string.algo_share_label),
        icon = Icons.Outlined.Sell,
        help = stringResource(R.string.algo_help_interests),
    ) {
        if (parts.isEmpty()) {
            RecommenderNote(stringResource(R.string.algo_profile_empty))
        } else {
            Donut(
                parts = parts,
                centerValue = counted.size,
                centerLabel = stringResource(R.string.algo_share_center),
                onSelect = { id ->
                    onAction(if (id == OTHERS) RecommenderSettingsAction.Open(RecommenderSection.Profile) else RecommenderSettingsAction.OpenInterest(id))
                },
            )
        }
        TextAction(stringResource(R.string.algo_adjust), { onAction(RecommenderSettingsAction.Open(RecommenderSection.Profile)) })
    }
}

private const val SHARE_PARTS = 10
private const val OTHERS = "others"
