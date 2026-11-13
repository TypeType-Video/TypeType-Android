package dev.typetype.android.feature.subscriptions.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.subscriptions.SubscriptionGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageGroupsSheet(viewModel: SubscriptionGroupsViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SubscriptionGroup?>(null) }
    var deleting by remember { mutableStateOf<SubscriptionGroup?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetTitle(stringResource(R.string.subscription_groups_manage), busy = state.busy)
            state.groups.forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.subscription_groups_channel_count,
                                group.channelCount,
                                group.channelCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { renaming = group }, enabled = !state.busy) {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.subscription_groups_rename, group.name))
                    }
                    IconButton(onClick = { deleting = group }, enabled = !state.busy) {
                        Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.subscription_groups_delete, group.name))
                    }
                }
            }
            if (state.loaded && state.groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.subscription_groups_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            NewGroupButton(enabled = !state.busy, onClick = { creating = true })
            ErrorLine(state.errorMessage)
        }
    }
    if (creating) {
        GroupNameDialog(stringResource(R.string.subscription_groups_new), "", onDismiss = { creating = false }) {
            viewModel.create(it)
            creating = false
        }
    }
    renaming?.let { group ->
        GroupNameDialog(stringResource(R.string.subscription_groups_rename, group.name), group.name, onDismiss = {
            renaming = null
        }) {
            viewModel.rename(group.id, it)
            renaming = null
        }
    }
    deleting?.let { group ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.subscription_groups_delete, group.name)) },
            text = { Text(stringResource(R.string.subscription_groups_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(group.id)
                    deleting = null
                }) { Text(stringResource(R.string.subscription_groups_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelGroupsSheet(
    channelName: String,
    viewModel: SubscriptionGroupsViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val memberOf = state.editedChannelGroupIds
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetTitle(stringResource(R.string.subscription_groups_for_channel, channelName), busy = state.busy)
            if (memberOf == null && state.errorMessage == null) {
                CircularProgressIndicator(modifier = Modifier.padding(20.dp))
            } else {
                state.groups.forEach { group ->
                    val checked = memberOf.orEmpty().contains(group.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                enabled = !state.busy && memberOf != null,
                                role = Role.Checkbox,
                                onValueChange = { viewModel.setMember(group.id, it) },
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(group.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            NewGroupButton(enabled = !state.busy, onClick = { creating = true })
            ErrorLine(state.errorMessage)
        }
    }
    if (creating) {
        GroupNameDialog(stringResource(R.string.subscription_groups_new), "", onDismiss = { creating = false }) {
            viewModel.create(it, thenAddChannel = state.editedChannelUrl)
            creating = false
        }
    }
}

@Composable
private fun SheetTitle(text: String, busy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NewGroupButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(horizontal = 8.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(stringResource(R.string.subscription_groups_new), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ErrorLine(message: String?) {
    message ?: return
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}
