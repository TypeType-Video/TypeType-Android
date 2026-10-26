package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SkeletonImage
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.domain.recommendations.DataCategory
import dev.typetype.android.domain.recommendations.DataItem

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.dataDetail(
    category: DataCategory,
    collectHistory: Boolean,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    val excludable = category.id in EXCLUDABLE
    item(key = "detail-${category.id}") {
        RecommenderGroup(label = dataLabel(category.id), icon = Icons.Outlined.Tune) {
            if (category.collect != null) {
                RecommenderRow(
                    label = stringResource(R.string.algo_collect),
                    description = stringResource(R.string.algo_collect_description),
                    icon = Icons.Outlined.Inbox,
                    help = stringResource(R.string.algo_help_collect),
                ) {
                    TypeTypeSwitch(checked = collectHistory, onCheckedChange = { onAction(RecommenderSettingsAction.SetCollect(it)) })
                }
            }
            RecommenderRow(
                label = stringResource(R.string.algo_use_for_home),
                description = stringResource(R.string.algo_use_for_home_description),
                icon = Icons.Outlined.Home,
                help = stringResource(R.string.algo_help_use),
                divider = excludable,
            ) {
                TypeTypeSwitch(checked = category.used, onCheckedChange = { onAction(RecommenderSettingsAction.ToggleUse(category.id)) })
            }
            if (excludable) {
                RecommenderRow(
                    label = stringResource(R.string.algo_keep_for),
                    description = stringResource(R.string.algo_keep_for_description),
                    icon = Icons.Outlined.Timer,
                    help = stringResource(R.string.algo_help_retention),
                    divider = false,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    RETENTION.forEach { days ->
                        FilterChip(
                            selected = category.retentionDays == days,
                            onClick = { onAction(RecommenderSettingsAction.SetRetention(category.id, days)) },
                            label = { Text(retentionLabel(days)) },
                        )
                    }
                }
            }
        }
    }
    item(key = "items-${category.id}") {
        RecommenderGroup(
            label = stringResource(R.string.algo_items_label, category.items.size, formatNumber(category.count.toDouble())),
            icon = Icons.AutoMirrored.Outlined.ViewList,
        ) {
            if (category.items.isEmpty()) RecommenderNote(stringResource(R.string.algo_empty))
            category.items.forEachIndexed { index, item ->
                DataItemRow(item, excludable) { onAction(RecommenderSettingsAction.ToggleExcluded(item.id)) }
                if (index < category.items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun DataItemRow(item: DataItem, excludable: Boolean, onToggle: () -> Unit) {
    val fade by animateFloatAsState(if (item.excluded) 0.4f else 1f, label = "excluded")
    val details = listOfNotNull(
        item.channel,
        ago(item.at).ifEmpty { null },
        if (item.viewings > 1) pluralStringResource(R.plurals.algo_viewings, item.viewings, item.viewings) else null,
        item.watchedPercent?.let { stringResource(R.string.algo_watched_share, it) },
        if (item.excluded) stringResource(R.string.algo_excluded) else null,
    ).joinToString(" · ")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!item.thumbnailUrl.isNullOrBlank()) {
            SkeletonImage(
                imageUrl = item.thumbnailUrl.orEmpty(),
                contentDescription = null,
                modifier = Modifier.width(96.dp).aspectRatio(16f / 9f).alpha(fade),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (item.excluded) TextDecoration.LineThrough else null),
                color = if (item.excluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (excludable) {
            TextAction(stringResource(if (item.excluded) R.string.algo_include else R.string.algo_exclude), onToggle)
        }
    }
}

private val RETENTION: List<Int?> = listOf(7, 30, 90, 180, 365, null)
private val EXCLUDABLE = setOf("history", "shorts_history", "favorites")
