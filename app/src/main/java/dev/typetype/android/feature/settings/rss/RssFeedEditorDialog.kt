package dev.typetype.android.feature.settings.rss

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.subscriptions.SubscriptionSummary

@Composable
internal fun RssFeedEditorDialog(
    state: RssFeedEditorState,
    subscriptions: List<SubscriptionSummary>,
    availableServiceIds: Set<Int>,
    isSaving: Boolean,
    onAction: (RssFeedsAction) -> Unit,
) {
    var channelQuery by rememberSaveable(state.feedId) { mutableStateOf("") }
    val visibleSubscriptions = subscriptions
        .filter {
            channelQuery.isBlank() ||
                it.name.contains(channelQuery, ignoreCase = true) ||
                it.channelUrl.contains(channelQuery, ignoreCase = true)
        }
        .sortedBy { it.name.lowercase() }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onAction(RssFeedsAction.DismissEditor) },
        title = {
            Text(
                stringResource(
                    if (state.feedId == null) R.string.rss_create_title else R.string.rss_edit_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(RssFeedsAction.SetName(it)) },
                    label = { Text(stringResource(R.string.rss_name)) },
                    enabled = !isSaving,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EditorSectionTitle(R.string.rss_scope_title)
                RadioRow(
                    text = stringResource(R.string.rss_scope_all),
                    selected = state.scope == RssFeedScope.All,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetScope(RssFeedScope.All)) },
                )
                RadioRow(
                    text = stringResource(R.string.rss_scope_selected),
                    selected = state.scope == RssFeedScope.Channels,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetScope(RssFeedScope.Channels)) },
                )
                if (state.scope == RssFeedScope.Channels) {
                    if (subscriptions.isEmpty()) {
                        Text(
                            stringResource(R.string.rss_no_subscriptions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        OutlinedTextField(
                            value = channelQuery,
                            onValueChange = { channelQuery = it },
                            label = { Text(stringResource(R.string.rss_search_subscriptions)) },
                            enabled = !isSaving,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(
                                R.string.rss_selected_channel_limit,
                                state.channelUrls.size,
                                MAX_CHANNELS,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        visibleSubscriptions.forEach { subscription ->
                            val selected = subscription.channelUrl in state.channelUrls
                            CheckboxRow(
                                text = subscription.name,
                                checked = selected,
                                enabled = !isSaving && (selected || state.channelUrls.size < MAX_CHANNELS),
                                onClick = {
                                    onAction(RssFeedsAction.ToggleChannel(subscription.channelUrl))
                                },
                            )
                        }
                        if (visibleSubscriptions.isEmpty()) {
                            Text(
                                stringResource(R.string.rss_no_matching_subscriptions),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider()
                EditorSectionTitle(R.string.rss_services_title)
                (availableServiceIds + state.serviceIds).sorted().forEach { serviceId ->
                    CheckboxRow(
                        text = stringResource(rssServiceName(serviceId)),
                        checked = serviceId in state.serviceIds,
                        enabled = !isSaving,
                        onClick = { onAction(RssFeedsAction.ToggleService(serviceId)) },
                    )
                }
                HorizontalDivider()
                EditorSectionTitle(R.string.rss_content_title)
                CheckboxRow(
                    text = stringResource(R.string.rss_include_videos),
                    checked = state.includeVideos,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetVideos(!state.includeVideos)) },
                )
                CheckboxRow(
                    text = stringResource(R.string.rss_include_shorts),
                    checked = state.includeShorts,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetShorts(!state.includeShorts)) },
                )
                CheckboxRow(
                    text = stringResource(R.string.rss_include_live),
                    checked = state.includeLive,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetLive(!state.includeLive)) },
                )
                CheckboxRow(
                    text = stringResource(R.string.rss_include_upcoming),
                    checked = state.includeUpcoming,
                    enabled = !isSaving,
                    onClick = { onAction(RssFeedsAction.SetUpcoming(!state.includeUpcoming)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(RssFeedsAction.Save) },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(RssFeedsAction.DismissEditor) },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun EditorSectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CheckboxRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            )
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun rssServiceName(serviceId: Int): Int = when (serviceId) {
    0 -> R.string.settings_default_service_youtube
    5 -> R.string.settings_default_service_bilibili
    6 -> R.string.settings_default_service_niconico
    else -> R.string.rss_service_unknown
}

private const val MAX_CHANNELS = 100
