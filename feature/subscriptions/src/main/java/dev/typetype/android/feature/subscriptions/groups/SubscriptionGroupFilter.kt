package dev.typetype.android.feature.subscriptions.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.subscriptions.SubscriptionFeedSelection

@Composable
internal fun SubscriptionGroupFilter(
    selection: SubscriptionFeedSelection,
    onSelect: (SubscriptionFeedSelection) -> Unit,
    viewModel: SubscriptionGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var managing by rememberSaveable { mutableStateOf(false) }
    val selectedGroup = (selection as? SubscriptionFeedSelection.Group)?.id
    LaunchedEffect(state.loaded, state.groups, selectedGroup) {
        if (state.loaded && selectedGroup != null && state.groups.none { it.id == selectedGroup }) {
            onSelect(SubscriptionFeedSelection.All)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = selection == SubscriptionFeedSelection.All,
                    onClick = { onSelect(SubscriptionFeedSelection.All) },
                    label = { Text(stringResource(R.string.subscription_groups_all)) },
                )
            }
            items(state.groups, key = { it.id }) { group ->
                FilterChip(
                    selected = selectedGroup == group.id,
                    onClick = { onSelect(SubscriptionFeedSelection.Group(group.id)) },
                    label = { Text(group.name) },
                )
            }
            if (state.groups.isNotEmpty()) {
                item(key = "ungrouped") {
                    FilterChip(
                        selected = selection == SubscriptionFeedSelection.Ungrouped,
                        onClick = { onSelect(SubscriptionFeedSelection.Ungrouped) },
                        label = { Text(stringResource(R.string.subscription_groups_ungrouped)) },
                    )
                }
            }
        }
        IconButton(onClick = { managing = true }) {
            Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.subscription_groups_manage))
        }
    }
    if (managing) ManageGroupsSheet(viewModel = viewModel, onDismiss = { managing = false })
}

@Composable
internal fun GroupNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val valid = name.trim().length in 1..MAX_GROUP_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_GROUP_NAME_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.subscription_groups_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = valid) {
                Text(stringResource(R.string.subscription_groups_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val MAX_GROUP_NAME_LENGTH = 100
