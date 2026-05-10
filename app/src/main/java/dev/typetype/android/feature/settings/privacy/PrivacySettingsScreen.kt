package dev.typetype.android.feature.settings.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R

private enum class PendingAction { History, SearchHistory, Unsubscribe }

@Composable
fun PrivacySettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.combinedState.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            TopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    PrivacyRow(
                        title = stringResource(R.string.settings_privacy_watch_history),
                        subtitle = stringResource(
                            R.string.settings_privacy_entries_count,
                            state.historyCount,
                        ),
                        actionLabel = stringResource(R.string.settings_privacy_clear),
                        actionEnabled = state.historyCount > 0,
                        onAction = { pending = PendingAction.History },
                    )
                }
                item {
                    PrivacyRow(
                        title = stringResource(R.string.settings_privacy_search_history),
                        subtitle = stringResource(
                            R.string.settings_privacy_entries_count,
                            state.searchHistoryCount,
                        ),
                        actionLabel = stringResource(R.string.settings_privacy_clear),
                        actionEnabled = state.searchHistoryCount > 0,
                        onAction = { pending = PendingAction.SearchHistory },
                    )
                }
                item {
                    PrivacyRow(
                        title = stringResource(R.string.settings_privacy_subscriptions),
                        subtitle = stringResource(
                            R.string.settings_privacy_channels_count,
                            state.subscriptionsCount,
                        ),
                        actionLabel = stringResource(R.string.settings_privacy_unsubscribe_all),
                        actionEnabled = state.subscriptionsCount > 0,
                        onAction = { pending = PendingAction.Unsubscribe },
                    )
                }
            }
        }
    }

    pending?.let { action ->
        val titleRes = when (action) {
            PendingAction.History -> R.string.settings_privacy_confirm_clear_history
            PendingAction.SearchHistory -> R.string.settings_privacy_confirm_clear_search
            PendingAction.Unsubscribe -> R.string.settings_privacy_confirm_unsubscribe
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(titleRes)) },
            text = { Text(stringResource(R.string.settings_privacy_confirm_irreversible)) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        PendingAction.History -> viewModel.clearWatchHistory()
                        PendingAction.SearchHistory -> viewModel.clearSearchHistory()
                        PendingAction.Unsubscribe -> viewModel.unsubscribeAll()
                    }
                    pending = null
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_privacy_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun PrivacyRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onAction,
            enabled = actionEnabled,
        ) {
            Text(
                text = actionLabel,
                color = if (actionEnabled) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
