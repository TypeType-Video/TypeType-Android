package dev.typetype.android.feature.settings.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.profiles.AccountProfile

@Composable
internal fun AccountProfilesSection(
    onProfileSwitched: () -> Unit,
    viewModel: AccountProfilesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.profileSwitched.collect { onProfileSwitched() } }
    if (!state.available) return
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<AccountProfile?>(null) }
    var deleting by remember { mutableStateOf<AccountProfile?>(null) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.account_profiles_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            state.profiles.forEach { profile ->
                ProfileRow(
                    profile = profile,
                    busy = state.busy,
                    onSwitch = { viewModel.switchTo(profile.id) },
                    onRename = { renaming = profile },
                    onSetDefault = { viewModel.setDefault(profile.id) },
                    onDelete = { deleting = profile },
                )
            }
            TextButton(onClick = { creating = true }, enabled = !state.busy) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.account_profiles_new), modifier = Modifier.padding(start = 8.dp))
            }
            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (creating) {
        ProfileNameDialog(stringResource(R.string.account_profiles_new), "", onDismiss = { creating = false }) {
            viewModel.create(it)
            creating = false
        }
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.account_profiles_delete)) },
            text = { Text(stringResource(R.string.account_profiles_delete_message, profile.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile.id)
                    deleting = null
                }) { Text(stringResource(R.string.account_profiles_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    renaming?.let { profile ->
        ProfileNameDialog(stringResource(R.string.account_profiles_rename), profile.name, onDismiss = {
            renaming = null
        }) {
            viewModel.rename(profile.id, it)
            renaming = null
        }
    }
}

@Composable
private fun ProfileRow(
    profile: AccountProfile,
    busy: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.bodyLarge)
            val labels = listOfNotNull(
                stringResource(R.string.account_profiles_current).takeIf { profile.isActive },
                stringResource(R.string.account_profiles_default).takeIf { profile.isDefault },
            )
            if (labels.isNotEmpty()) {
                Text(
                    text = labels.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!profile.isActive) {
            FilledTonalButton(onClick = onSwitch, enabled = !busy) {
                Text(stringResource(R.string.account_profiles_switch))
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.account_profiles_actions, profile.name))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.account_profiles_rename)) },
                    onClick = { menuOpen = false; onRename() },
                )
                if (!profile.isDefault) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.account_profiles_make_default)) },
                        onClick = { menuOpen = false; onSetDefault() },
                    )
                }
                if (!profile.isActive) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.account_profiles_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_PROFILE_NAME_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.account_profiles_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.subscription_groups_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val MAX_PROFILE_NAME_LENGTH = 40
