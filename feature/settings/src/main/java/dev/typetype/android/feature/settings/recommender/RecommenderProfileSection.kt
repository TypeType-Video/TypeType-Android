package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.ProfileChannel

internal fun LazyListScope.profileSection(
    state: RecommenderSettingsState,
    showAll: Boolean,
    onShowAll: () -> Unit,
    onOpenChannel: (ProfileChannel) -> Unit,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    if (!state.preferences.enabled) {
        item { RecommenderNote(stringResource(R.string.algo_overview_off)) }
        return
    }
    val profile = when (val loaded = state.profile) {
        Loadable.Failed -> {
            item { LoadFailed { onAction(RecommenderSettingsAction.Retry) } }
            return
        }
        is Loadable.Loaded -> loaded.value
        else -> {
            item { RecommenderNote(stringResource(R.string.algo_loading)) }
            return
        }
    }
    item(key = "interests") {
        RecommenderGroup(
            label = stringResource(R.string.algo_interests_label),
            icon = Icons.Outlined.Sell,
            help = stringResource(R.string.algo_help_interests),
        ) {
            AddInterest(state, onAction)
            if (profile.groups.isEmpty()) RecommenderNote(stringResource(R.string.algo_profile_empty))
        }
    }
    val shown = shownInterests(state, showAll)
    items(shown, key = { "interest-${it.id}" }) { group ->
        InterestCard(group, state, highlighted = group.id == state.highlight, onAction = onAction)
    }
    val hidden = profile.groups.size - shown.size
    if (hidden > 0) {
        item(key = "interests-more") { TextAction(pluralStringResource(R.plurals.algo_interests_more, hidden, hidden), onShowAll) }
    }
    if (profile.channels.isNotEmpty()) {
        item(key = "channels") {
            RecommenderGroup(
                label = stringResource(R.string.algo_channels_label),
                icon = Icons.Outlined.People,
                help = stringResource(R.string.algo_help_channels),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    items(profile.channels, key = { it.id }) { channel ->
                        ChannelChip(channel, ignored = channel.id in state.preferences.ignoredSignals) { onOpenChannel(channel) }
                    }
                }
            }
        }
    }
    item(key = "recent") {
        RecommenderGroup(
            label = stringResource(R.string.algo_recent_label),
            icon = Icons.Outlined.Schedule,
            help = stringResource(R.string.algo_help_recent),
        ) {
            if (profile.sessionBoosts.isEmpty()) RecommenderNote(stringResource(R.string.algo_recent_empty))
            val now = System.currentTimeMillis()
            profile.sessionBoosts.forEach { boost ->
                val minutes = ((boost.expiresAt - now) / MINUTE_MS).toInt().coerceAtLeast(0)
                RecommenderRow(
                    label = boost.label,
                    description = if (minutes > 0) {
                        pluralStringResource(R.plurals.algo_counts_for, minutes, minutes)
                    } else {
                        stringResource(R.string.algo_no_longer_counts)
                    },
                )
            }
            if (profile.sessionBoosts.isNotEmpty()) {
                RecommenderRow(
                    label = stringResource(R.string.algo_forget_recent),
                    description = stringResource(R.string.algo_forget_recent_description),
                    divider = false,
                ) {
                    TextAction(stringResource(R.string.algo_forget), { onAction(RecommenderSettingsAction.ForgetRecent) })
                }
            }
        }
    }
}

internal fun shownInterests(state: RecommenderSettingsState, showAll: Boolean): List<InterestGroup> {
    val ranked = state.profile.value?.groups.orEmpty().sortedByDescending { it.weight }
    if (showAll) return ranked
    val preferences = state.preferences
    return ranked.filterIndexed { index, group ->
        index < INTERESTS_SHOWN || group.custom || group.id in preferences.pinnedGroups ||
            group.id in preferences.mutedGroups || group.id == state.highlight
    }
}

@Composable
private fun AddInterest(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAction(RecommenderSettingsAction.AddInterest(draft))
        draft = ""
    }
    Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(MAX_INTEREST_LENGTH) },
            enabled = state.canAddInterest,
            placeholder = { Text(stringResource(R.string.algo_interest_add_placeholder), maxLines = 1) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) submit() }),
            trailingIcon = {
                IconButton(onClick = submit, enabled = state.canAddInterest && draft.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.algo_interest_add))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (state.canAddInterest) {
                stringResource(R.string.algo_interest_add_hint)
            } else {
                pluralStringResource(R.plurals.algo_interest_full, MAX_CUSTOM_INTERESTS, MAX_CUSTOM_INTERESTS)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val INTERESTS_SHOWN = 12
private const val MINUTE_MS = 60_000L
