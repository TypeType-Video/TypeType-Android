package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.PageTrace

@Composable
internal fun LastHome(state: RecommenderSettingsState, open: Boolean, onToggle: () -> Unit, onRetry: () -> Unit) {
    val trace = when (val loaded = state.lastHome) {
        Loadable.Failed -> return LoadFailed(onRetry)
        is Loadable.Loaded -> loaded.value
        else -> return RecommenderNote(stringResource(R.string.algo_loading))
    }
    if (trace == null) return RecommenderNote(stringResource(R.string.algo_last_home_none))
    val built = listOf(ago(trace.builtAt), stringResource(R.string.algo_computed_by, state.algorithmName(trace.algorithm)))
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    RecommenderFold(
        icon = Icons.Outlined.Home,
        label = stringResource(R.string.algo_last_home_fold),
        description = built,
        summary = stringResource(R.string.algo_last_home_summary, formatNumber(trace.candidates.toDouble()), formatNumber(trace.shown.toDouble())),
        open = open,
        onToggle = onToggle,
        divider = false,
    ) { TraceDetails(trace) }
}

@Composable
private fun TraceDetails(trace: PageTrace) {
    var rowsOpen by rememberSaveable { mutableStateOf(false) }
    var scoresOpen by rememberSaveable { mutableStateOf(false) }
    val dropped = trace.dropped.values.sum()
    RecommenderRow(
        label = pluralStringResource(R.plurals.algo_found, trace.candidates, trace.candidates),
        description = traceSources(trace),
        icon = Icons.Outlined.Search,
    )
    RecommenderRow(
        label = pluralStringResource(R.plurals.algo_dropped, dropped, dropped),
        description = trace.dropped.map { (why, count) -> dropLabel(why, count) }.joinToString(" · "),
        icon = Icons.Outlined.FilterAlt,
    )
    RecommenderRow(
        label = pluralStringResource(R.plurals.algo_shown, trace.shown, trace.shown),
        description = pluralStringResource(R.plurals.algo_shown_rows, trace.rows.size, trace.rows.size),
        icon = Icons.Outlined.Visibility,
    )
    RecommenderRow(
        label = pluralStringResource(R.plurals.algo_rows_count, trace.rows.size, trace.rows.size),
        description = stringResource(R.string.algo_rows_description),
        icon = Icons.AutoMirrored.Outlined.ViewList,
    ) { TextAction(stringResource(if (rowsOpen) R.string.algo_hide else R.string.algo_show), { rowsOpen = !rowsOpen }) }
    AnimatedVisibility(visible = rowsOpen) {
        Column(modifier = Modifier.padding(start = 32.dp)) {
            trace.rows.forEach { row ->
                RecommenderRow(
                    label = row.title,
                    description = listOf(row.why, pluralStringResource(R.plurals.algo_videos, row.videos, row.videos))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                )
            }
        }
    }
    RecommenderRow(
        label = pluralStringResource(R.plurals.algo_scores_label, trace.scores.size, trace.scores.size),
        description = stringResource(R.string.algo_scores_description),
        icon = Icons.Outlined.FormatListNumbered,
        divider = scoresOpen,
    ) { TextAction(stringResource(if (scoresOpen) R.string.algo_hide else R.string.algo_show), { scoresOpen = !scoresOpen }) }
    AnimatedVisibility(visible = scoresOpen) {
        Column(modifier = Modifier.padding(start = 32.dp)) {
            trace.scores.forEach { score ->
                val factors = score.factors.filterValues { it != 0.0 }.map { (factor, value) -> "$factor ${formatNumber(value)}" }
                RecommenderRow(
                    label = score.title.ifBlank { score.url },
                    description = (listOf(score.channel, score.row) + factors).filter { it.isNotBlank() }.joinToString(" · "),
                ) {
                    Text(formatNumber(score.total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun traceSources(trace: PageTrace): String {
    fun count(kind: String) = trace.sources.count { it.kind == kind }
    return listOfNotNull(
        if (count("subscription_feed") > 0) stringResource(R.string.algo_source_subscriptions) else null,
        count("related").takeIf { it > 0 }?.let { pluralStringResource(R.plurals.algo_source_related, it, it) },
        count("channel_videos").takeIf { it > 0 }?.let { pluralStringResource(R.plurals.algo_source_channels, it, it) },
        count("search").takeIf { it > 0 }?.let { pluralStringResource(R.plurals.algo_source_search, it, it) },
        if (count("trending") > 0) stringResource(R.string.algo_source_trending) else null,
    ).joinToString(", ")
}
