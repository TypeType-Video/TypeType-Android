package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.recommenderInterests(
    state: RecommenderSettingsState,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    val preferences = state.preferences
    item { SettingsSectionHeader(stringResource(R.string.algo_interests_label)) }
    item(key = "custom-interests") {
        var draft by rememberSaveable { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.algo_interest_add_hint), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preferences.customInterests.forEach { interest ->
                    InputChip(
                        selected = true,
                        onClick = { onAction(RecommenderSettingsAction.RemoveInterest(interest)) },
                        label = { Text(interest) },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.algo_interest_remove))
                        },
                    )
                }
            }
            if (state.canAddInterest) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(MAX_INTEREST_LENGTH) },
                        placeholder = { Text(stringResource(R.string.algo_interest_add_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onAction(RecommenderSettingsAction.AddInterest(draft))
                            draft = ""
                        },
                        enabled = draft.isNotBlank(),
                    ) { Text(stringResource(R.string.algo_interest_add)) }
                }
            } else {
                Text(
                    pluralStringResource(R.plurals.algo_interest_full, MAX_CUSTOM_INTERESTS, MAX_CUSTOM_INTERESTS),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    val groups = state.interests.sortedByDescending { it.weight }.take(VISIBLE_GROUPS)
    items(groups, key = { "interest-${it.id}" }) { group ->
        val pinned = group.id in preferences.pinnedGroups
        val muted = group.id in preferences.mutedGroups
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(group.label, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = pinned,
                    onClick = { onAction(RecommenderSettingsAction.TogglePinned(group.id)) },
                    label = { Text(stringResource(R.string.algo_pin)) },
                )
                FilterChip(
                    selected = muted,
                    onClick = { onAction(RecommenderSettingsAction.ToggleMuted(group.id)) },
                    label = { Text(stringResource(R.string.algo_hide)) },
                )
            }
        }
    }
}

internal fun LazyListScope.recommenderData(
    state: RecommenderSettingsState,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    if (state.families.isEmpty()) return
    item { SettingsSectionHeader(stringResource(R.string.algo_data_label)) }
    item { Text(stringResource(R.string.algo_data_intro), style = MaterialTheme.typography.bodySmall) }
    items(state.families, key = { "family-${it.id}" }) { family ->
        SwitchRow(
            title = stringResource(familyLabel(family.id)),
            subtitle = pluralStringResource(R.plurals.algo_data_count, family.count, family.count),
            checked = family.used,
            onCheckedChange = { onAction(RecommenderSettingsAction.SetFamilyUsed(family.id, it)) },
        )
    }
}

private fun familyLabel(id: String): Int = when (id) {
    "history" -> R.string.algo_cat_history
    "shorts_history" -> R.string.algo_cat_shorts_history
    "favorites" -> R.string.algo_cat_favorites
    "subscriptions" -> R.string.algo_cat_subscriptions
    "blocked" -> R.string.algo_cat_blocked
    "not_interested" -> R.string.algo_cat_not_interested
    else -> R.string.algo_cat_other
}

private const val VISIBLE_GROUPS = 15
