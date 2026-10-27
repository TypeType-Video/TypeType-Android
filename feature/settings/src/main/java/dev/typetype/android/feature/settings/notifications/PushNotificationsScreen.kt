package dev.typetype.android.feature.settings.notifications

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.domain.push.PushRegistrationStatus
import dev.typetype.android.feature.settings.SettingsDetailTopBar
import org.unifiedpush.android.connector.UnifiedPush

@Composable
fun PushNotificationsRoute(
    onNavigateBack: () -> Unit,
    onOpenChannels: () -> Unit,
    viewModel: PushNotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PushNotificationsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onOpenChannels = onOpenChannels,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun PushNotificationsScreen(
    state: PushSettingsState,
    onNavigateBack: () -> Unit,
    onOpenChannels: () -> Unit,
    onAction: (PushSettingsAction) -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onAction(PushSettingsAction.Toggle)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_push_title),
                onNavigateBack = onNavigateBack,
            )
            PushToggleRow(
                state = state,
                onToggle = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !state.status.isRegistered() &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onAction(PushSettingsAction.Toggle)
                    }
                },
            )
            ChannelsRow(onClick = onOpenChannels)
            StatusRow(label = stringResource(statusLabel(state.status)))
            var hasDistributor by remember { mutableStateOf(UnifiedPush.getDistributors(context).isNotEmpty()) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                hasDistributor = UnifiedPush.getDistributors(context).isNotEmpty()
            }
            if (state.status == PushRegistrationStatus.MissingDistributor || !hasDistributor) {
                DistributorRow(hasDistributor = hasDistributor)
            }
            Text(
                text = stringResource(R.string.push_local_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp, 4.dp),
            )
            if (state.capabilityEnabled && state.deviceCount > 0) {
                StatusRow(
                    label = stringResource(R.string.push_devices_count, state.deviceCount, state.maxDevices),
                )
            }
            if (state.status.isRegistered()) {
                BatteryHintRow()
            }
            if (state.showError) {
                Text(
                    text = stringResource(R.string.push_error_generic),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(20.dp, 12.dp),
                )
            }
        }
    }
}

@Composable
private fun PushToggleRow(
    state: PushSettingsState,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = state.capabilityEnabled && !state.inFlight, onClick = onToggle)
            .padding(20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_push_title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_push_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TypeTypeSwitch(
            checked = state.status.isRegistered(),
            onCheckedChange = null,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ChannelsRow(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp, 12.dp),
    ) {
        Text(
            text = stringResource(R.string.push_channels_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.push_channels_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusRow(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(20.dp, 12.dp),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DistributorRow(hasDistributor: Boolean) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.push_install_hint),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(20.dp, 12.dp),
    )
    Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, SUNUP_URL.toUri())) }) {
            Text(stringResource(R.string.push_get_sunup))
        }
        if (hasDistributor) {
            TextButton(
                onClick = {
                    (context as? Activity)?.let { activity ->
                        UnifiedPush.tryUseCurrentOrDefaultDistributor(activity) { }
                    }
                },
            ) {
                Text(stringResource(R.string.push_choose_distributor))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun BatteryHintRow() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(PowerManager::class.java)
    if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) return
    Text(
        text = stringResource(R.string.push_battery_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(20.dp, 12.dp),
    )
    TextButton(
        onClick = {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
        },
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text(stringResource(R.string.push_battery_open_settings))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private const val SUNUP_URL = "https://unifiedpush.org/users/distributors/sunup/"

private fun PushRegistrationStatus.isRegistered(): Boolean =
    this == PushRegistrationStatus.Registered || this == PushRegistrationStatus.Registering

private fun statusLabel(status: PushRegistrationStatus): Int = when (status) {
    PushRegistrationStatus.Registered -> R.string.push_status_active
    PushRegistrationStatus.Registering -> R.string.push_status_connecting
    PushRegistrationStatus.MissingDistributor -> R.string.push_status_missing_distributor
    PushRegistrationStatus.Unavailable -> R.string.push_status_unavailable
    PushRegistrationStatus.ActionRequired -> R.string.push_status_action_required
    PushRegistrationStatus.Failed -> R.string.push_status_failed
    PushRegistrationStatus.Disabled -> R.string.push_status_disabled
}
