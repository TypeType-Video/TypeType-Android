package dev.typetype.android.feature.settings.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.ChannelAvatar
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun ChannelNotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: ChannelNotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChannelNotificationsScreen(state = state, onNavigateBack = onNavigateBack, onAction = viewModel::onAction)
}

@Composable
internal fun ChannelNotificationsScreen(
    state: ChannelNotificationsState,
    onNavigateBack: () -> Unit,
    onAction: (ChannelNotificationsAction) -> Unit,
) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = context.notificationsAllowed() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = context.notificationsAllowed()
    }
    val dispatch: (ChannelNotificationsAction) -> Unit = { action ->
        if (!allowed && action.turnsOn(state) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onAction(action)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.push_channels_title),
                onNavigateBack = onNavigateBack,
            )
            when {
                state.isLoading && state.channels.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.loadFailed && state.channels.isEmpty() -> LoadFailed(onRetry = { onAction(ChannelNotificationsAction.Retry) })
                else -> ChannelList(
                    state = state,
                    allowed = allowed,
                    onAllow = context::openNotificationSettings,
                    onAction = dispatch,
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: ChannelNotificationsState,
    allowed: Boolean,
    onAllow: () -> Unit,
    onAction: (ChannelNotificationsAction) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "summary") {
            Column(modifier = Modifier.padding(20.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.push_channels_summary,
                        state.channels.size,
                        state.enabledCount,
                        state.channels.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onAction(ChannelNotificationsAction.SetAll(true)) },
                        enabled = !state.bulkInFlight && state.enabledCount < state.channels.size,
                    ) { Text(stringResource(R.string.push_channels_enable_all)) }
                    OutlinedButton(
                        onClick = { onAction(ChannelNotificationsAction.SetAll(false)) },
                        enabled = !state.bulkInFlight && state.enabledCount > 0,
                    ) { Text(stringResource(R.string.push_channels_disable_all)) }
                }
                if (state.bulkInFlight) {
                    LinearProgressIndicator(
                        progress = { state.bulkDone / state.bulkTotal.coerceAtLeast(1).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.push_channels_progress, state.bulkDone, state.bulkTotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.showError) {
                    Text(
                        text = stringResource(R.string.push_channels_update_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!allowed && state.enabledCount > 0) NotificationsBlocked(onAllow = onAllow)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (state.channels.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.push_channels_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp, 16.dp),
                )
            }
        }
        items(state.channels, key = { it.channelUrl }) { channel ->
            ChannelBellRow(
                channel = channel,
                enabled = !state.bulkInFlight && !channel.pending,
                onToggle = { onAction(ChannelNotificationsAction.Toggle(channel.channelUrl)) },
            )
        }
    }
}

@Composable
private fun ChannelBellRow(channel: ChannelBell, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = channel.enabled, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChannelAvatar(avatarUrl = channel.avatarUrl, name = channel.name, size = 40.dp)
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TypeTypeSwitch(checked = channel.enabled, onCheckedChange = null)
    }
}

@Composable
private fun NotificationsBlocked(onAllow: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.push_notifications_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAllow) { Text(stringResource(R.string.push_notifications_allow)) }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.push_channels_load_failed), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

private fun ChannelNotificationsAction.turnsOn(state: ChannelNotificationsState): Boolean = when (this) {
    is ChannelNotificationsAction.SetAll -> enabled
    is ChannelNotificationsAction.Toggle -> state.channels.firstOrNull { it.channelUrl == channelUrl }?.enabled == false
    ChannelNotificationsAction.Retry -> false
}

private fun Context.notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    startActivity(intent)
}
