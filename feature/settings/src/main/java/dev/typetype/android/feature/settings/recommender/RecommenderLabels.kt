package dev.typetype.android.feature.settings.recommender

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
internal fun dataLabel(id: String): String = when (id) {
    "history" -> stringResource(R.string.algo_cat_history)
    "shorts_history" -> stringResource(R.string.algo_cat_shorts_history)
    "events" -> stringResource(R.string.algo_cat_events)
    "search" -> stringResource(R.string.algo_cat_search)
    "favorites" -> stringResource(R.string.algo_cat_favorites)
    "watch_later" -> stringResource(R.string.algo_cat_watch_later)
    "subscriptions" -> stringResource(R.string.algo_cat_subscriptions)
    "blocked" -> stringResource(R.string.algo_cat_blocked)
    "not_interested" -> stringResource(R.string.algo_cat_not_interested)
    "shown" -> stringResource(R.string.algo_cat_shown)
    else -> id
}

internal fun dataIcon(id: String): ImageVector = when (id) {
    "history" -> Icons.Outlined.Tv
    "shorts_history" -> Icons.Outlined.Movie
    "favorites" -> Icons.Outlined.FavoriteBorder
    "subscriptions" -> Icons.Outlined.NotificationsNone
    "blocked" -> Icons.Outlined.Block
    "not_interested" -> Icons.Outlined.ThumbDown
    else -> Icons.Outlined.Storage
}

@Composable
internal fun reasonLabel(kind: String): String = stringResource(
    when (kind) {
        "watched", "same_channel" -> R.string.algo_part_recent
        "continue" -> R.string.algo_part_continue
        "watch_again" -> R.string.algo_part_watch_again
        "subscriptions", "new_from_subscriptions" -> R.string.algo_part_subscriptions
        "topic" -> R.string.algo_part_topics
        "discovery" -> R.string.algo_part_discovery
        "trending" -> R.string.algo_part_trending
        else -> R.string.algo_part_recommended
    },
)

@Composable
internal fun dropLabel(why: String, count: Int): String = when (why) {
    "seen" -> pluralStringResource(R.plurals.algo_drop_seen, count, count)
    "blocked" -> pluralStringResource(R.plurals.algo_drop_blocked, count, count)
    "channel_cap" -> pluralStringResource(R.plurals.algo_drop_channel_cap, count, count)
    "muted" -> pluralStringResource(R.plurals.algo_drop_muted, count, count)
    "excluded" -> pluralStringResource(R.plurals.algo_drop_excluded, count, count)
    "not_interested" -> pluralStringResource(R.plurals.algo_drop_not_interested, count, count)
    "short" -> pluralStringResource(R.plurals.algo_drop_short, count, count)
    "obscure" -> pluralStringResource(R.plurals.algo_drop_obscure, count, count)
    else -> "$count $why"
}

internal data class GroupText(val title: Int, val help: Int, val icon: ImageVector)

internal fun groupText(group: String): GroupText = when (group) {
    "sources" -> GroupText(R.string.algo_group_sources, R.string.algo_group_sources_help, Icons.Outlined.Inbox)
    "weights" -> GroupText(R.string.algo_group_weights, R.string.algo_group_weights_help, Icons.Outlined.Scale)
    "decay" -> GroupText(R.string.algo_group_decay, R.string.algo_group_decay_help, Icons.Outlined.HourglassEmpty)
    "rows" -> GroupText(R.string.algo_group_rows, R.string.algo_group_rows_help, Icons.AutoMirrored.Outlined.ViewList)
    "exploration" -> GroupText(R.string.algo_group_exploration, R.string.algo_group_exploration_help, Icons.Outlined.Explore)
    "randomness" -> GroupText(R.string.algo_group_randomness, R.string.algo_group_randomness_help, Icons.Outlined.Casino)
    else -> GroupText(R.string.algo_group_other, R.string.algo_group_other_help, Icons.Outlined.Tune)
}

@Composable
internal fun ago(at: Long, now: Long = System.currentTimeMillis()): String {
    if (at <= 0L) return ""
    val hours = ((now - at).coerceAtLeast(0L) / HOUR_MS.toDouble()).roundToInt()
    return when {
        hours < 1 -> stringResource(R.string.algo_ago_now)
        hours < 48 -> stringResource(R.string.algo_ago_hours, hours)
        else -> (hours / 24.0).roundToInt().let { days -> pluralStringResource(R.plurals.algo_ago_days, days, days) }
    }
}

@Composable
internal fun retentionLabel(days: Int?): String = when {
    days == null -> stringResource(R.string.algo_retention_forever)
    days == 365 -> stringResource(R.string.algo_retention_year)
    days >= 180 -> (days / 30.0).roundToInt().let { months -> pluralStringResource(R.plurals.algo_retention_months, months, months) }
    else -> stringResource(R.string.algo_retention_days_short, days)
}

@Composable
internal fun itemCount(count: Int): String = pluralStringResource(R.plurals.algo_data_count, count, formatNumber(count.toDouble()))

@Composable
internal fun basedOn(count: Int): String = pluralStringResource(R.plurals.algo_based_on, count, count)

@Composable
internal fun formatParameter(parameter: AlgorithmParameter, value: Any): String = when {
    value is Boolean -> stringResource(if (value) R.string.algo_on else R.string.algo_off)
    parameter is AlgorithmParameter.Number && parameter.unit == "%" ->
        "${((value as? Number)?.toDouble() ?: 0.0).times(100).roundToInt()} %"
    parameter is AlgorithmParameter.Number && parameter.unit == "min" ->
        stringResource(R.string.algo_minutes, formatNumber((value as? Number)?.toDouble() ?: 0.0))
    else -> formatNumber((value as? Number)?.toDouble() ?: 0.0)
}

internal fun formatNumber(value: Double): String =
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(value)

internal fun formatPercent(share: Double): String =
    NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }.format(share)

internal val LEVEL_LABELS = listOf(
    R.string.algo_level_none,
    R.string.algo_level_low,
    R.string.algo_level_medium,
    R.string.algo_level_high,
)

private const val HOUR_MS = 3_600_000L
